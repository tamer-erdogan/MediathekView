/*
 * MediathekView
 * Copyright (C) 2008 W. Xaver
 * W.Xaver[at]googlemail.com
 * http://zdfmediathk.sourceforge.net/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package mediathek.controller;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import mediathek.daten.Daten;
import mediathek.tool.DatumZeit;
import mediathek.tool.GuiFunktionen;
import mediathek.tool.ListenerMediathekView;

public class ErledigteAbos {

    private final static String TRENNER = "  |###|  ";
    private final static String PAUSE = " |#| ";
    private HashSet<String> listeErledigteAbos;
    private LinkedList<String[]> listeErledigteAbosSortDate = new LinkedList<>();

    public ErledigteAbos() {
        listeErledigteAbos = new HashSet<String>() {

            @Override
            public void clear() {
                listeErledigteAbosSortDate.clear();
                super.clear();
            }
        };
        listeBauen();
    }

    /**
     * Return the Path object to the downloadAbo file
     *
     * @return Path object to downloadAbo file
     */
    private Path getDownloadAboFilePath() {
        Path aboFilePath = null;
        try {
            aboFilePath = Daten.getSettingsDirectory().resolve("downloadAbos.txt");
            if (Files.notExists(aboFilePath)) {
                aboFilePath = Files.createFile(aboFilePath);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return aboFilePath;
    }

    public synchronized void alleLoeschen() {
        listeErledigteAbos.clear();
        try {
            Path downloadAboFilePath = getDownloadAboFilePath();
            if (Files.exists(downloadAboFilePath)) {
                Files.delete(downloadAboFilePath);
            }
        } catch (IOException ignored) {
        }

        ListenerMediathekView.notify(ListenerMediathekView.EREIGNIS_LISTE_ERLEDIGTE_ABOS, ErledigteAbos.class.getSimpleName());
    }

    public synchronized boolean urlAusLogfileLoeschen(String urlFilm) {
        //Logfile einlesen, entsprechende Zeile Filtern und dann Logfile überschreiben
        //wenn die URL im Logfiel ist, dann true zurück
        String zeile;
        boolean gefunden = false;
        LinkedList<String> liste = new LinkedList<>();

        //Use Automatic Resource Management
        Path aboFilePath = getDownloadAboFilePath();
        if (Files.notExists(aboFilePath)) {
            return false;
        }

        try (LineNumberReader in = new LineNumberReader(new InputStreamReader(Files.newInputStream(aboFilePath)))) {
            while ((zeile = in.readLine()) != null) {
                if (getUrlAusZeile(zeile)[2].equals(urlFilm)) {
                    gefunden = true; //nur dann muss das Logfile auch geschrieben werden
                } else {
                    liste.add(zeile);
                }
            }
            in.close();
        } catch (Exception ex) {
            Log.fehlerMeldung(281006874, Log.FEHLER_ART_PROG, "LogDownload.urlAusLogfileLoeschen-1", ex);
        }

        //und jetzt wieder schreiben, wenn nötig
        if (gefunden) {
            try (BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(getDownloadAboFilePath())))) {
                for (String entry : liste) {
                    bufferedWriter.write(entry + "\n");
                }
                bufferedWriter.flush();
                bufferedWriter.close();
            } catch (Exception ex) {
                Log.fehlerMeldung(566277080, Log.FEHLER_ART_PROG, "LogDownload.urlAusLogfileLoeschen-3", ex);
            }
        }
        listeErledigteAbos.clear();
        listeBauen();

        ListenerMediathekView.notify(ListenerMediathekView.EREIGNIS_LISTE_ERLEDIGTE_ABOS, ErledigteAbos.class.getSimpleName());
        return gefunden;
    }

    public synchronized boolean zeileSchreiben(String thema, String titel, String url) {
        boolean ret = false;
        String text;
        String datum = DatumZeit.getHeute_dd_MM_yyyy();
        listeErledigteAbos.add(url);
        listeErledigteAbosSortDate.add(new String[]{datum, titel, url});

        //Automatic Resource Management
        try (BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(getDownloadAboFilePath(), StandardOpenOption.APPEND)))) {
            thema = GuiFunktionen.textLaenge(25, putzen(thema), false /* mitte */, false /*addVorne*/);
            titel = GuiFunktionen.textLaenge(30, putzen(titel), false /* mitte */, false /*addVorne*/);
            text = datum + PAUSE + thema + PAUSE + titel + TRENNER + url + "\n";
            bufferedWriter.write(text);
            bufferedWriter.flush();
            bufferedWriter.close();
            ret = true;
        } catch (Exception ex) {
            Log.fehlerMeldung(945258023, Log.FEHLER_ART_PROG, "LogDownload.zeileSchreiben-1", ex);
        }

        ListenerMediathekView.notify(ListenerMediathekView.EREIGNIS_LISTE_ERLEDIGTE_ABOS, ErledigteAbos.class.getSimpleName());
        return ret;
    }

    // eigener Thread!!
    public synchronized void zeileSchreiben(ArrayList<String[]> list) {
        new Thread(new zeileSchreiben_(list)).start();
    }

    private class zeileSchreiben_ implements Runnable {

        ArrayList<String[]> l;

        public zeileSchreiben_(ArrayList<String[]> ll) {
            l = ll;
        }

        @Override
        public synchronized void run() {
            zeileSchreiben(l);
        }

        public synchronized boolean zeileSchreiben(ArrayList<String[]> list) {

            boolean ret = false;
            String text;
            String zeit = DatumZeit.getHeute_dd_MM_yyyy();
            String thema, titel, url;
            try (BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(getDownloadAboFilePath(), StandardOpenOption.APPEND)))) {
                for (String[] a : list) {
                    thema = a[0];
                    titel = a[1];
                    url = a[2];
                    listeErledigteAbos.add(url);
                    listeErledigteAbosSortDate.add(new String[]{zeit, titel, url});
                    thema = GuiFunktionen.textLaenge(25, putzen(thema), false /* mitte */, false /*addVorne*/);
                    titel = GuiFunktionen.textLaenge(30, putzen(titel), false /* mitte */, false /*addVorne*/);
                    text = zeit + PAUSE + thema + PAUSE + titel + TRENNER + url + "\n";
                    bufferedWriter.write(text);
                    ret = true;
                }
                bufferedWriter.flush();
                bufferedWriter.close();
            } catch (Exception ex) {
                ret = false;
                Log.fehlerMeldung(945258023, Log.FEHLER_ART_PROG, "LogDownload.zeileSchreiben-1", ex);
            }
            ListenerMediathekView.notify(ListenerMediathekView.EREIGNIS_LISTE_ERLEDIGTE_ABOS, ErledigteAbos.class.getSimpleName());
            return ret;
        }
    }

    public synchronized boolean urlPruefen(String urlFilm) {
        //wenn url gefunden, dann true zurück
        return listeErledigteAbos.contains(urlFilm);
    }

    public synchronized Object[][] getObjectData() {
        Object[][] object;
        int i = 0;
        Iterator<String[]> iterator = listeErledigteAbosSortDate.iterator();
        object = new Object[listeErledigteAbosSortDate.size()][3];
        while (iterator.hasNext()) {
            object[i] = iterator.next();
            ++i;
        }
        return object;
    }

    // ==============================
    // private
    // ==============================
    private void listeBauen() {
        //LinkedList mit den URLs aus dem Logfile bauen
        Path downloadAboFilePath = getDownloadAboFilePath();
        //use Automatic Resource Management
        try (LineNumberReader in = new LineNumberReader(new InputStreamReader(Files.newInputStream(downloadAboFilePath)))) {
            String zeile;
            while ((zeile = in.readLine()) != null) {
                String[] s = getUrlAusZeile(zeile);
                listeErledigteAbos.add(s[2]);
                listeErledigteAbosSortDate.add(s);
            }
            in.close();
        } catch (Exception ex) {
            Log.fehlerMeldung(926362547, Log.FEHLER_ART_PROG, ErledigteAbos.class.getName(), ex);
        }
    }

    private String putzen(String s) {
        s = s.replace("\n", "");
        s = s.replace("|", "");
        s = s.replace(TRENNER, "");
        return s;
    }

    private String[] getUrlAusZeile(String zeile) {
        String url = "", titel = "", datum = "";
        int a1;

        try {
            if (zeile.contains(TRENNER)) {
                //neues Logfile-Format
                a1 = zeile.lastIndexOf(TRENNER);
                a1 += TRENNER.length();
                url = zeile.substring(a1);
                // titel
                titel = zeile.substring(zeile.lastIndexOf(PAUSE) + PAUSE.length(), zeile.lastIndexOf(TRENNER));
                datum = zeile.substring(0, zeile.indexOf(PAUSE));
            } else {
                url = zeile;
            }
        } catch (Exception ex) {
            Log.fehlerMeldung(398853224, Log.FEHLER_ART_PROG, "ErledigteAbos.getUrlAusZeile: " + zeile, ex);
        }
        return new String[]{datum, titel, url};
    }
}
