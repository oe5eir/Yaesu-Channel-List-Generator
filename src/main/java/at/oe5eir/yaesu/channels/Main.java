package at.oe5eir.yaesu.channels;

/*
 *  Copyright (C) 2023 OE5EIR @ https://www.oe5eir.at/
 *
 *  Licensed under the GNU General Public License v3.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *
 *  You may obtain a copy of the License at
 *      https://www.gnu.org/licenses/gpl-3.0.en.html
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import at.oe5eir.yaesu.channels.config.OffsetDirection;
import at.oe5eir.yaesu.channels.config.ToneMode;
import at.oe5eir.yaesu.channels.config.Channel;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 * Generator for Austrian Repeater List for Yaesu FT3D Radios
 * Parameter: Output File
 */
public final class Main {
    public static void main(String[] args) {
        long time = System.currentTimeMillis();

        System.setProperty("file.encoding", "UTF-8");

        switch (args.length) {
            case 1:
                try {
                    start(args[0], false);
                } catch (Exception e) {
                    System.err.println(Util.getCause(e));
                }
                break;
            case 2:
                try {
                    start(args[0], true);
                } catch (Exception e) {
                    System.err.println(Util.getCause(e));
                }
                break;
            case 0:
                new GUI().show();
                return;
            default:
                System.out.println("Parameter 1: output filename\r\n(optional) Parameter 2: city as channel name instead on mountain");
                return;
        }

        System.out.println(String.format("Completed in %.1f seconds.", (System.currentTimeMillis() - time) / 1000.0f));
    }

    public static void start(String file, boolean cityAsName) throws IOException {
        System.out.println("Getting data from OEVSV Repeater API...");

        JSONArray fm70cm = getJsonData("https://repeater.oevsv.at/api/trx?status=eq.active&type_of_station=eq.repeater_voice&fm=eq.true&band=eq.70cm");
        JSONArray fm2m = getJsonData("https://repeater.oevsv.at/api/trx?status=eq.active&type_of_station=eq.repeater_voice&fm=eq.true&band=eq.2m");
        JSONArray c4fm70cm = getJsonData("https://repeater.oevsv.at/api/trx?status=eq.active&type_of_station=eq.repeater_voice&c4fm=eq.true&band=eq.70cm");
        JSONArray c4fm2m = getJsonData("https://repeater.oevsv.at/api/trx?status=eq.active&type_of_station=eq.repeater_voice&c4fm=eq.true&band=eq.2m");

        System.out.println("Processing dataset...");

        List<Channel> fm70cmList = convertDataFM(fm70cm, cityAsName);
        List<Channel> fm2mList = convertDataFM(fm2m, cityAsName);
        List<Channel> c4fm70cmList = convertDataC4FM(c4fm70cm, cityAsName);
        List<Channel> c4fm2mList = convertDataC4FM(c4fm2m, cityAsName);

        System.out.println("Writing output file...");

        Locale.setDefault(Locale.US);

        File csvFile = new File(file);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile))) {
            int i = 1;

            for (Channel c : fm70cmList) {
                writer.write(i++ + "," + c + "\n");
            }

            for (Channel c : fm2mList) {
                writer.write(i++ + "," + c + "\n");
            }

            for (Channel c : c4fm70cmList) {
                writer.write(i++ + "," + c + "\n");
            }

            for (Channel c : c4fm2mList) {
                writer.write(i++ + "," + c + "\n");
            }

            for (;i <= 900; i++) {
                writer.write(i + ",,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,0\n");
            }
        }

        System.out.println("List Location: " + csvFile.getAbsoluteFile());
    }

    private static JSONArray internalDownloadJsonArray(String _url, int timeout) throws IOException {
        URL url = new URL(_url);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setConnectTimeout(timeout);
        con.setReadTimeout(timeout);
        con.setInstanceFollowRedirects(false);

        int status = con.getResponseCode();
        if (status != 200)
            System.out.println("Status: " + status);

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();

        con.disconnect();

        return new JSONArray(content.toString());
    }

    private static JSONArray getJsonData(String apiUrl) throws IOException {
        return internalDownloadJsonArray(apiUrl, 5000);
    }

    private static String getCity(String location) throws IOException {
        String url = "https://repeater.oevsv.at/api/site?site_name=eq." + location.replace(" ", "%20");
        JSONArray content = internalDownloadJsonArray(url, 3000);
        return ((JSONObject) content.get(0)).getString("city");
    }

    private static List<Channel> convertDataFM(JSONArray data, boolean cityAsName) throws IOException {
        List<Channel> list = new ArrayList<>();

        for (Object row : data) {
            String callsign = (String) ((JSONObject) row).get("callsign");
            String location = (String) ((JSONObject) row).get("site_name");
            String city = getCity(location);
            Object txo = ((JSONObject) row).get("frequency_tx");
            double tx = fixBullshitToDouble(txo);
            Object rxo = ((JSONObject) row).get("frequency_rx");
            double rx = fixBullshitToDouble(rxo);
            Object ctcssTxObj = ((JSONObject) row).get("ctcss_tx");
            Double ctcssTx = fixBullshitToDouble(ctcssTxObj);
            Object ctcssRxObj = ((JSONObject) row).get("ctcss_rx");
            Double ctcssRx = fixBullshitToDouble(ctcssRxObj);

            String name = callsign + " ";

            if (cityAsName) {
                name += fixString(city,9);
            } else {
                name += fixString(location,9);
            }

            ToneMode toneMode = ctcssRx != null ? ToneMode.TONESQL : ToneMode.OFF;

            // filter out repeaters which have abnormal shift
            double offsetFrequency = Math.abs(rx-tx);
            if (!(equals(offsetFrequency, 7.6) || equals(offsetFrequency, 0.6)))
                continue;

            // show warning when Rx and Tx CTCSS don't match as FT3D can't have separate config for Rx and Tx
            if (!compareDouble(ctcssTx, ctcssRx))
                System.err.println(String.format("%16s: CTCSS not matching! Rx %5.1f | Tx %5.1f", name, ctcssRx, ctcssTx));

            list.add(new Channel(tx, rx, OffsetDirection.RPTM, name, toneMode, ctcssRx));
        }

        list.sort(new ChannelComparator());
        return list;
    }

    private static List<Channel> convertDataC4FM(JSONArray data, boolean cityAsName) throws IOException {
        List<Channel> list = new ArrayList<>();

        for (Object row : data) {
            String callsign = (String) ((JSONObject) row).get("callsign");
            String location = (String) ((JSONObject) row).get("site_name");
            String city = getCity(location);
            Object txo = ((JSONObject) row).get("frequency_tx");
            double tx = fixBullshitToDouble(txo);
            Object rxo = ((JSONObject) row).get("frequency_rx");
            double rx = fixBullshitToDouble(rxo);

            String name = callsign + " ";

            if (cityAsName) {
                name += fixString(city,9);
            } else {
                name += fixString(location,9);
            }

            double offsetFrequency = Math.abs(rx-tx);

            // filter out crappy API data
            if (!(equals(offsetFrequency, 7.6) || equals(offsetFrequency, 0.6)))
                continue;

            list.add(new Channel(tx, rx, OffsetDirection.RPTM, name));
        }

        list.sort(new ChannelComparator());
        return list;
    }

    private static String fixString(String s, int len) {
        String str = s;
        str = str.replaceAll("\\d","");
        str = str.replace("ß","ss");
        str = str.replace("Ä","Ae");
        str = str.replace("Ö","Oe");
        str = str.replace("Ü","Ue");
        str = str.replace("ä","ae");
        str = str.replace("ö","oe");
        str = str.replace("ü","ue");
        str = Util.toCamelCase(str, true, ' ', '-', '.');
        str = str.trim();

        if (str.length() > len)
            return str.substring(0, len - 1) + ".";
        else
            return str;
    }

    private static Double fixBullshitToDouble(Object obj) {
        if (obj instanceof Double)
            return (Double) obj;
        else if (obj instanceof Integer)
            return ((Integer) obj).doubleValue();
        else if (obj instanceof BigDecimal)
            return ((BigDecimal) obj).doubleValue();
        else
            return null;
    }

    public static boolean compareDouble(Double a, Double b) {
        if (a == null && b == null)
            return true;

        if (a != null && b == null)
            return false;

        if (a == null && b != null)
            return false;

        return a.equals(b);
    }

    public static boolean equals(double a, double b){
        return a == b ? true : Math.abs(a - b) < 0.000001;
    }
}
