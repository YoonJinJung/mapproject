'use strict';

/**
 * Space Threat Monitoring Dashboard - BFF Lambda
 * Endpoints (query parameter 'type'로 분기):
 *   ?start_date=&end_date=          → NASA NeoWs 소행성 피드
 *   ?type=apod                      → NASA APOD (오늘의 우주 이미지)
 *
 * Environment Variables:
 *   NASA_API_KEY – https://api.nasa.gov 에서 발급
 *
 * Open-source: https (Node.js built-in)
 */

const https = require('https');

const NASA_NEOWS_BASE = 'https://api.nasa.gov/neo/rest/v1/feed';
const NASA_APOD_BASE  = 'https://api.nasa.gov/planetary/apod';

function getTodayString() {
  return new Date().toISOString().split('T')[0];
}

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
        try { resolve(JSON.parse(raw)); }
        catch (e) { reject(new Error('Failed to parse NASA API JSON response')); }
      });
    });
    req.setTimeout(10000, () => {
      req.destroy(new Error('NASA API request timed out after 10 s'));
    });
    req.on('error', reject);
  });
}

function mapAsteroids(nearEarthObjects) {
  const result = [];
  for (const date of Object.keys(nearEarthObjects)) {
    const dailyList = nearEarthObjects[date];
    if (!Array.isArray(dailyList)) continue;
    for (const neo of dailyList) {
      const approach =
        Array.isArray(neo.close_approach_data) && neo.close_approach_data.length > 0
          ? neo.close_approach_data[0] : null;
      const diamKm = neo.estimated_diameter?.kilometers ?? {};
      result.push({
        id: neo.id ?? '',
        name: (neo.name ?? 'Unknown').replace(/[()]/g, '').trim(),
        absoluteMagnitude: typeof neo.absolute_magnitude_h === 'number' ? neo.absolute_magnitude_h : null,
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
  result.sort((a, b) => parseFloat(a.missDistanceKm) - parseFloat(b.missDistanceKm));
  return result;
}

function buildResponse(statusCode, body) {
  return {
    statusCode,
    headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' },
    body: JSON.stringify(body),
  };
}

// ─── APOD 핸들러 ──────────────────────────────────────────────────────────────
async function handleApod(apiKey) {
  console.log('[INFO] Fetching APOD');
  try {
    const data = await httpsGet(`${NASA_APOD_BASE}?api_key=${apiKey}`);
    return buildResponse(200, {
      date:        data.date        ?? '',
      title:       data.title       ?? '',
      explanation: data.explanation ?? '',
      url:         data.url         ?? '',
      hdurl:       data.hdurl       ?? null,
      mediaType:   data.media_type  ?? 'image',
      copyright:   data.copyright   ?? null,
    });
  } catch (err) {
    console.error('[APOD ERROR]', err.message);
    return buildResponse(502, { error: 'Failed to fetch APOD', message: err.message });
  }
}

// ─── NeoWs 핸들러 ─────────────────────────────────────────────────────────────
async function handleNeows(apiKey, params) {
  const today     = getTodayString();
  const startDate = params.start_date ?? today;
  const endDate   = params.end_date   ?? today;

  const dateRegex = /^\d{4}-\d{2}-\d{2}$/;
  if (!dateRegex.test(startDate) || !dateRegex.test(endDate)) {
    return buildResponse(400, { error: 'Invalid date format', message: 'YYYY-MM-DD required.' });
  }
  const diffDays = (new Date(endDate) - new Date(startDate)) / (1000 * 60 * 60 * 24);
  if (diffDays < 0 || diffDays > 7) {
    return buildResponse(400, { error: 'Invalid date range', message: 'Range must be 0–7 days.' });
  }

  let nasaData;
  try {
    nasaData = await httpsGet(
      `${NASA_NEOWS_BASE}?start_date=${startDate}&end_date=${endDate}&api_key=${apiKey}`
    );
  } catch (err) {
    console.error('[NeoWs ERROR]', err.message);
    return buildResponse(502, { error: 'Failed to fetch NeoWs data', message: err.message });
  }

  let asteroids;
  try {
    asteroids = mapAsteroids(nasaData.near_earth_objects ?? {});
  } catch (err) {
    return buildResponse(500, { error: 'Failed to parse NeoWs response', message: err.message });
  }

  const hazardousCount = asteroids.filter((a) => a.isPotentiallyHazardous).length;
  console.log(`[INFO] Returning ${asteroids.length} asteroids (${hazardousCount} hazardous).`);
  return buildResponse(200, {
    fetchDate: today,
    summary: { totalCount: asteroids.length, hazardousCount, startDate, endDate },
    asteroids,
  });
}

// ─── Lambda 핸들러 ────────────────────────────────────────────────────────────
exports.handler = async (event) => {
  const apiKey = process.env.NASA_API_KEY;
  if (!apiKey) {
    console.error('[CONFIG ERROR] NASA_API_KEY is not set.');
    return buildResponse(500, { error: 'NASA_API_KEY not configured on server.' });
  }

  const params = event.queryStringParameters ?? {};

  // type=apod 이면 APOD 핸들러로 분기
  if (params.type === 'apod') {
    return await handleApod(apiKey);
  }

  // 기본: NeoWs 소행성 피드
  return await handleNeows(apiKey, params);
};
