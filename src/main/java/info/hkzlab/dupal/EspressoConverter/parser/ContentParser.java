package info.hkzlab.dupal.EspressoConverter.parser;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import info.hkzlab.dupal.EspressoConverter.devices.*;
import info.hkzlab.dupal.EspressoConverter.states.*;

public class ContentParser {
    private ContentParser() {};

    public static PALSpecs getPALType(JSONObject root) {
        String palName = root.getJSONObject("header").getJSONObject("PAL").getString("type");

        switch(palName.toUpperCase()) {
            case "PAL16L8":
                return new PAL16L8Specs();
            case "PAL10L8":
                return new PAL10L8Specs();
            default:
                return null;
        }
    }
    
    public static int extractIOasOutMask(JSONObject root) {
        if(!root.getJSONObject("header").getJSONObject("PAL").has("IOsAsOUT")) return 0;

        return root.getJSONObject("header").getJSONObject("PAL").getInt("IOsAsOUT");
    }

    public static SimpleState[] extractSimpleStates(JSONObject root) {
        ArrayList<SimpleState> ssList = new ArrayList<>();

        if(!root.has("states")) return null;
        JSONArray ssArray = root.getJSONArray("states");

        for(int idx = 0; idx < ssArray.length(); idx++) {
            JSONObject ss = ssArray.getJSONObject(idx);
            ssList.add(new SimpleState(ss.getInt("inputs"), ss.getInt("outputs"), ss.getInt("hiz")));
        }

        return ssList.toArray(new SimpleState[ssList.size()]);
    }

    public static OLink[] extractOLinks(JSONObject root) {
        ArrayList<OLink> olList = new ArrayList<>();

        if(!root.has("oLinks")) return null;
        JSONArray olArray = root.getJSONArray("oLinks");

        for(int idx = 0; idx < olArray.length(); idx++) {
            JSONObject ol = olArray.getJSONObject(idx);
            OutStatePins src = new OutStatePins(ol.getJSONObject("source").getInt("outputs"), ol.getJSONObject("source").getInt("hiz"));
            OutStatePins dst = new OutStatePins(ol.getJSONObject("destination").getInt("outputs"), ol.getJSONObject("destination").getInt("hiz"));

            olList.add(new OLink(ol.getInt("inputs"), src, dst));
        }

        return olList.toArray(new OLink[olList.size()]);
    }

    public static RLink[] extractRLinks(JSONObject root) {
        ArrayList<RLink> rlList = new ArrayList<>();

        if(!root.has("rLinks")) return null;
        JSONArray rlArray = root.getJSONArray("rLinks");

        for(int idx = 0; idx < rlArray.length(); idx++) {
            JSONObject rl = rlArray.getJSONObject(idx);
            OutStatePins src = new OutStatePins(rl.getJSONObject("source").getInt("outputs"), rl.getJSONObject("source").getInt("hiz"));
            OutStatePins mid = new OutStatePins(rl.getJSONObject("middle").getInt("outputs"), rl.getJSONObject("middle").getInt("hiz"));
            OutStatePins dst = new OutStatePins(rl.getJSONObject("destination").getInt("outputs"), rl.getJSONObject("source").getInt("hiz"));

            rlList.add(new RLink(rl.getInt("inputs"), src, mid, dst));
        }
        return rlList.toArray(new RLink[rlList.size()]);
    }
}
