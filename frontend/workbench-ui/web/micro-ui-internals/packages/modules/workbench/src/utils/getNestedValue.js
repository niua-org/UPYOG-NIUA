/**
 * Safely resolves a nested dot-notation path from an object, returning a fallback if undefined.
 * Mirrors setNestedValue helper.
 * 
 * @param {Object} obj - Source object to read from.
 * @param {string|string[]} path - Dot-separated path string (e.g. "colors.primary.main").
 * @param {*} [fallback] - Fallback value if path is undefined.
 * @returns {*} Resolved value or fallback.
 */
export const getNestedValue = (obj, path, fallback) => {
  if (!obj || !path) return fallback;
  const keys = Array.isArray(path) ? path : path.split(".");
  let current = obj;
  for (let i = 0; i < keys.length; i++) {
    if (current === null || current === undefined) return fallback;
    current = current[keys[i]];
  }
  return current !== undefined ? current : fallback;
};

export default getNestedValue;
