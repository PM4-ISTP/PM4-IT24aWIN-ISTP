package com.pm4.istp.course.db.entities;

/**
 * Controls how many attempts a student gets for MULTIPLE_CHOICE challenges in a course.
 *
 * <ul>
 *   <li>{@link #ONCE} – the student may pick exactly one option; the challenge is marked completed
 *       regardless of whether the answer is correct (graded / Praktikum mode).
 *   <li>{@link #UNLIMITED} – the student may keep trying until they choose the correct option; the
 *       challenge is only marked completed on a correct answer (self-learning mode).
 * </ul>
 */
public enum McAttemptsMode {
  ONCE,
  UNLIMITED
}
