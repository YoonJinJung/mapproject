/**
 * Space Threat Monitoring Dashboard - BFF Lambda (ES Module)
 * ?type=apod        → NASA APOD
 * (default)         → NASA NeoWs 소행성 피드
 */

import https from 'https';

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
        res.resume(); return;
      }
      let raw = '';
      res.setEncoding('utf8');
      res.on('data', (chunk) => { raw += chunk; });
      res.on('end', () => {
        try { resolve(JSON.parse(raw)); }
        catch (e) { reject(new Error('Failed to parse NASA API JSON')); }
      });
    });
    req.setTimeout(10000, () => req.destroy(new Error('NASA API timed out')));
    req.on('error', reject);
  });
}

function mapAsteroids(nearEarthObjects) {
  const result = [];
  for (const date of Object.keys(nearEarthObjects)) {
    const list = nearEarthObjects[date];
    if (!Array.isArray(list)) continue;
    for (const neo of list) {
      const approach = Array.isArray(neo.close_approach_data) && neo.close_approach_data.length > 0
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

async function handleApod(apiKey) {
  console.log('[INFO] Fetching APOD');
  try {
    const data = await httpsGet(`${NASA_APOD_BASE}?api_key=${apiKey}`);
    return buildResponse(200, {
      date: data.date ?? '', title: data.title ?? '',
      explanation: data.explanation ?? '', url: data.url ?? '',
      hdurl: data.hdurl ?? null, mediaType: data.media_type ?? 'image',
      copyright: data.copyright ?? null,
    });
  } catch (err) {
    return buildResponse(502, { error: 'Failed to fetch APOD', message: err.message });
  }
}

async function handleNeows(apiKey, params) {
  const today = getTodayString();
  const startDate = params.start_date ?? today;
  const endDate   = params.end_date   ?? today;
  const dateRegex = /^\d{4}-\d{2}-\d{2}$/;
  if (!dateRegex.test(startDate) || !dateRegex.test(endDate))
    return buildResponse(400, { error: 'Invalid date format' });
  const diff = (new Date(endDate) - new Date(startDate)) / 86400000;
  if (diff < 0 || diff > 7)
    return buildResponse(400, { error: 'Date range must be 0–7 days' });
  try {
    const nasaData = await httpsGet(
      `${NASA_NEOWS_BASE}?start_date=${startDate}&end_date=${endDate}&api_key=${apiKey}`
    );
    const asteroids = mapAsteroids(nasaData.near_earth_objects ?? {});
    const hazardousCount = asteroids.filter(a => a.isPotentiallyHazardous).length;
    return buildResponse(200, {
      fetchDate: today,
      summary: { totalCount: asteroids.length, hazardousCount, startDate, endDate },
      asteroids,
    });
  } catch (err) {
    return buildResponse(502, { error: 'Failed to fetch NeoWs', message: err.message });
  }
}

export const handler = async (event) => {
  const apiKey = process.env.NASA_API_KEY;
  if (!apiKey) return buildResponse(500, { error: 'NASA_API_KEY not configured.' });
  const params = event.queryStringParameters ?? {};
  if (params.type === 'apod') return await handleApod(apiKey);
  return await handleNeows(apiKey, params);
};
