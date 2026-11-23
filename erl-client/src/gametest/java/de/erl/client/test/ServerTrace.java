/*
 * This file is part of ERL-Client.
 * Licensed under the GNU General Public License v3.0.
 */

package de.erl.client.test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Alles, was der Server vom Client mitbekommt (Custom-Payloads, Known Packs), für die Spuren-Prüfung im Test. */
public final class ServerTrace {
    public static final List<String> RECEIVED = new CopyOnWriteArrayList<>();

    private ServerTrace() {
    }
}
