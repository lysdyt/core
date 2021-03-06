package io.tetrapod.core.tasks;

/**
 * Zero arg function
 * @author paulm
 *         Created: 6/28/16
 */
@FunctionalInterface
public interface Func0<TRet> {
   TRet apply();
}
