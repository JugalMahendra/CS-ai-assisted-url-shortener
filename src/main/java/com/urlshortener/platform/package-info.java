/**
 * Platform / infrastructure concerns: application configuration, health and observability wiring,
 * and other cross-feature plumbing that is not domain logic.
 *
 * <p>Module boundary (modular monolith): feature modules (shortening, redirect, analytics,
 * validation, abuse) are introduced in later tasks and must interact through published service
 * interfaces rather than each other's internals.
 */
package com.urlshortener.platform;
