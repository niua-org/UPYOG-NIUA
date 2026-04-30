/**
 * Timer-based seat locking with pluggable storage (PostgreSQL vs Redis) and optional Redis→DB fallback.
 * <p>
 * Java 17 usage: {@code record} DTOs for API bodies and rows, {@code sealed} hierarchies for exhaustive
 * outcomes, {@code switch} expressions on those hierarchies, and {@code var} for local temporaries where
 * types are obvious — together they reduce null/branch bugs and make illegal states unrepresentable.
 * <p>
 * Configure with {@code seat-lock.provider=db|redis|redis-with-db-fallback}.
 */
package org.upyog.chb.seatlock;
