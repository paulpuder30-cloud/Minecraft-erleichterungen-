package de.erl.client.lib.starscript.utils;

import de.erl.client.lib.starscript.Starscript;
import de.erl.client.lib.starscript.value.Value;

/** Interface used for {@link Value#function(SFunction)}. */
public interface SFunction {
    Value run(Starscript ss, int agrCount);
}
