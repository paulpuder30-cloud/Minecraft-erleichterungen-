package de.erl.client.lib.starscript.utils;

import de.erl.client.lib.starscript.Starscript;

/** Used in {@link Starscript#getCompletions(String, int, CompletionCallback)}. */
public interface CompletionCallback {
    void onCompletion(String completion, boolean function);
}
