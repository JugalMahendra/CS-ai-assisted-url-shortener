-- V3: dedicated sequence for short-code generation.
-- Codes are Base62 encodings of sequence values. Starting at 62^6 (56,800,235,584) guarantees
-- 7-character codes until the sequence passes 62^7 (~3.5e12 values later). Sequence values are unique
-- and monotonic, so generated codes are collision-free by construction; the UNIQUE constraint on
-- short_link.code remains the safety net (including against collisions with custom aliases).
CREATE SEQUENCE short_code_seq START WITH 56800235584 INCREMENT BY 1 NO CYCLE;
