/**
 * Space Threat Monitoring Dashboard - BFF Lambda Function
 *
 * Purpose : NASA NeoWs API를 호출하여 모바일 앱에 최적화된 경량 JSON을 반환하는
 *           Backend-For-Frontend 서버입니다.
 *
 * Runtime : Node.js 18.x (AWS Lambda)
 * Endpoint: GET /feed?start_date=YYYY-MM-DD&end_date=YYYY-MM-DD
 *           (파라미터 생략 시 오늘 날짜 기준 단일 조회)
 *
 * Environment Variables:
 *   NASA_API_KEY  – NASA Open APIs (https://api.nasa.gov) 에서 발급받은 API 키
 *                   절대로 코드에 하드코딩하지 않습니다.
 *
 * Open-source libraries used (Node.js built-in only, 외부 패키지 없음):
 *   - https (Node.js built-in)
 *
 * NASA NeoWs API: https://api.nasa.gov/neo/rest/v1/feed
 */

'use strict';

const https = require('https');

const NASA_API_BASE = 'https://api.nasa.gov/neo/rest/v1/feed';

// ─── 헬퍼: 오늘 날짜를 YYYY-MM-DD 형식으로 반환 ───────────────────────────────
function getTodayString() {
  return new Date().toISOString().split('T')[0];
}

// ─── 헬퍼: HTTPS GET 요청 Promise 래퍼 ────────────────────────────────────────
function httpsGet(url) {
  return new Promise((resolve, reject) => {
    const req = https.get(url, (res) => {
      if (res.statusCode < 200 || res.statusCode >= 300) {
        reject(new Error(`NASA API responded with status ${res.statusCode}`));
        res.resume();
        return;
      }

      let raw = '';
      res.setEncoding('utf8');
      res.on('data', (chunk) => { raw += chunk; });
      res.on('end', () => {
        try {
          resolve(JSON.parse(raw));
        } catch (e) {
          reject(new Error('Failed to parse NASA API JSON response'));
        }
      });
    });

    req.setTimeout(10000, () => {
      req.destroy(new Error('NASA API request timed out after 10 s'));
    });

    req.on('error', reject);
  });
}

// ─── 핵심: NASA 원시 응답 → 경량 소행성 배열로 변환 ──────────────────────────
function mapAsteroids(nearEarthObjects) {
  const result = [];

  for (const date of Object.keys(nearEarthObjects)) {
    const dailyList = nearEarthObjects[date];
    if (!Array.isArray(dailyList)) continue;

    for (const neo of dailyList) {
      // 근지점 접근 데이터 (첫 번째 항목 사용)
      const approach = Array.isArray(neo.close_approach_data) && neo.close_approach_data.length > 0
        ? neo.close_approach_data[0]
        : null;

      // 추정 직경 (킬로미터)
      const diamKm = neo.estimated_diameter?.kilometers ?? {};

      result.push({
        id: neo.id ?? '',
        name: (neo.name ?? 'Unknown').replace(/[()]/g, '').trim(), // 괄호 제거 후 정리
        absoluteMagnitude: typeof neo.absolute_magnitude_h === 'number'
          ? neo.absolute_magnitude_h
          : null,
        estimatedDiameterMinKm: diamKm.estimated_diameter_min ?? 0,
        estimatedDiameterMaxKm: diamKm.estimated_diameter_max ?? 0,
        isPotentiallyHazardous: neo.is_potentially_hazardous_asteroid === true,
        closeApproachDate: approach?.close_approach_date ?? date,
        relativeVelocityKmh: approach?.relative_velocity?.kilometers_per_hour ?? '0',
        missDistanceKm: approach?.miss_distance?.kilometers ?? '0',
        orbitingBody: approach?.orbiting_body ?? 'Earth',
        nasaJplUrl: neo.nasa_jpl_url ?? null,
      });
    }
  }

  // 지구와의 거리(missDistanceKm) 오름차순 정렬 (가장 가까운 천체가 맨 위)
  result.sort((a, b) => parseFloat(a.missDistanceKm) - parseFloat(b.missDistanceKm));

  return result;
}

// ─── 공통 HTTP 응답 빌더 ──────────────────────────────────────────────────────
function buildResponse(statusCode, body) {
  return {
    statusCode,
    headers: {
      'Content-Type': 'application/json',
      'Access-Control-Allow-Origin': '*',   // CORS (개발 편의)
    },
    body: JSON.stringify(body),
  };
}

// ─── Lambda 핸들러 ─────────────────────────────────────────────────────────────
exports.handler = async (event) => {
  // 1. 환경변수에서 API 키 읽기 (절대 하드코딩 금지)
  const apiKey = process.env.NASA_API_KEY;
  if (!apiKey) {
    console.error('[CONFIG ERROR] NASA_API_KEY environment variable is not set.');
    return buildResponse(500, {
      error: 'Server configuration error',
      message: 'NASA_API_KEY is not configured on the server.',
    });
  }

  // 2. 쿼리 파라미터 파싱 (없으면 오늘 날짜)
  const params = event.queryStringParameters ?? {};
  const today = getTodayString();
  const startDate = params.start_date ?? today;
  const endDate   = params.end_date   ?? today;

  // 날짜 형식 간단 검증 (YYYY-MM-DD)
  const dateRegex = /^\d{4}-\d{2}-\d{2}$/;
  if (!dateRegex.test(startDate) || !dateRegex.test(endDate)) {
    return buildResponse(400, {
      error: 'Invalid date format',
      message: 'start_date and end_date must be in YYYY-MM-DD format.',
    });
  }

  // NeoWs API 조회 범위는 최대 7일
  const diffDays = (new Date(endDate) - new Date(startDate)) / (1000 * 60 * 60 * 24);
  if (diffDays < 0 || diffDays > 7) {
    return buildResponse(400, {
      error: 'Invalid date range',
      message: 'Date range must be between 0 and 7 days.',
    });
  }

  // 3. NASA NeoWs API 호출
  const nasaUrl = `${NASA_API_BASE}?start_date=${startDate}&end_date=${endDate}&api_key=${apiKey}`;
  console.log(`[INFO] Fetching NASA NeoWs: start=${startDate}, end=${endDate}`);

  let nasaData;
  try {
    nasaData = await httpsGet(nasaUrl);
  } catch (err) {
    console.error('[NASA API ERROR]', err.message);
    return buildResponse(502, {
      error: 'Failed to fetch data from NASA API',
      message: err.message,
    });
  }

  // 4. 데이터 파싱 및 변환
  let asteroids;
  try {
    asteroids = mapAsteroids(nasaData.near_earth_objects ?? {});
  } catch (err) {
    console.error('[PARSE ERROR]', err.message);
    return buildResponse(500, {
      error: 'Failed to parse NASA API response',
      message: err.message,
    });
  }

  // 5. 요약 통계 계산
  const hazardousCount = asteroids.filter((a) => a.isPotentiallyHazardous).length;

  // 6. 최종 응답 조립
  const responseBody = {
    fetchDate: today,
    summary: {
      totalCount: asteroids.length,
      hazardousCount,
      startDate,
      endDate,
    },
    asteroids,
  };

  console.log(`[INFO] Returning ${asteroids.length} asteroids (${hazardousCount} hazardous).`);
  return buildResponse(200, responseBody);
};
