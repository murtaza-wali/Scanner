package com.ontrac.warehouse.OnTrac;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.text.format.DateFormat;
import android.text.format.DateUtils;

import com.ontrac.warehouse.BaseApplication;
import com.ontrac.warehouse.BuildConfig;
import com.ontrac.warehouse.Entities.User;
import com.ontrac.warehouse.Entities.ZipCode;
import com.ontrac.warehouse.LoginActivity;
import com.ontrac.warehouse.Utilities.Preferences;
import com.ontrac.warehouse.Utilities.Strings;
import com.ontrac.warehouse.Utilities.UX.General;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;

import static android.content.Context.BATTERY_SERVICE;
import static j2html.TagCreator.b;
import static j2html.TagCreator.body;
import static j2html.TagCreator.br;
import static j2html.TagCreator.div;
import static j2html.TagCreator.p;

public class Utilities {

    public static boolean isValidLaserShipTracking(String code)
    {
        boolean result = false;
            if(!Strings.IsNullOrWhiteSpace(code))
            {
                if ((code.length() >= 15) && (code.indexOf("|") == -1))
                {
                    if ((code.substring(0, 1).compareTo("1") == 0) && (code.substring(1, 2).compareTo("L") == 0) && (code.substring(2, 3).compareTo("S") == 0)) {
                        result = true;
                    }
                }
            }
        return result;
    }

    public static boolean isValidOnTracTracking(String code) {
        boolean result = false;

        if (Strings.IsNullOrWhiteSpace(code)) {
            return result;
        }

        if (HasInvalidCharacter(code)) {
            return result;
        }

        int codeLength = code.length();

        String firstCharacter = code.substring(0,1);
        if ((codeLength == 12 | codeLength == 15) && (firstCharacter.compareToIgnoreCase("B" ) == 0 | firstCharacter.compareToIgnoreCase("C") == 0 | firstCharacter.compareToIgnoreCase("D")==0 | firstCharacter.compareToIgnoreCase("X")==0))
//        if ((codeLength == 12 | codeLength == 15) && (firstCharacter.compareToIgnoreCase("B" ) == 0 | firstCharacter.compareToIgnoreCase("C") == 0 | firstCharacter.compareToIgnoreCase("D")==0))
        {
            boolean goOn = false;
            boolean flip = false;
            int x = 0;
            int evenBit = 0;
            int oddBit = 0;

            Strings.TrimCharArrayResult codeSections = Strings.TrimCharArray(code.toCharArray(), 1, 1);

            int prefixValue = GetBarcodePrefixValue(codeSections.Left.get(0));
            char prefixChar = Integer.toString(prefixValue).charAt(0);

            codeSections.Center.add(0, prefixChar);

            for (char c : codeSections.Center) {

                Integer parsed = Strings.TryParseInt(Character.toString(c));

                if (parsed != null) {
                    x = parsed;

                    if (flip)
                    {
                        evenBit += x;
                    }
                    else
                    {
                        oddBit += x;
                    }
                    flip = !flip;

                    goOn = true;
                }
                else {
                    goOn = false;
                    break;
                }
            }

            if (goOn)
            {
                evenBit *= 2;

                double checkBit = ((Math.floor((oddBit + evenBit) * 0.1) + 1) * 10) - (oddBit + evenBit);
                if (checkBit == 10)
                {
                    checkBit = 0;
                }

                Integer parsed = Strings.TryParseInt(Character.toString(codeSections.Right.get(0)));

                if (parsed != null) {
                    x = parsed;
                    if (checkBit == x)
                    {
                        result = true;
                    }
                }
            }
        }

        return result;
    }

    public static int calcCheckDigit(String code)
    {
        int checkDigit = -1;
        boolean goOn = false;
        boolean flip = false;
        int x = 0;
        int evenBit = 0;
        int oddBit = 0;

        Strings.TrimCharArrayResult codeSections = Strings.TrimCharArray(code.toCharArray(), 0, 1);

        for (char c : codeSections.Center) {

            Integer parsed = GetBarcodePrefixValue(c);

            if (parsed != null) {
                x = parsed;

                if (flip)
                {
                    evenBit += x;
                }
                else
                {
                    oddBit += x;
                }
                flip = !flip;

                goOn = true;
            }
            else {
                goOn = false;
                break;
            }
        }

        if (goOn)
        {
            evenBit *= 2;

            double checkBit = ((Math.floor((oddBit + evenBit) * 0.1) + 1) * 10) - (oddBit + evenBit);
            if (checkBit == 10)
            {
                checkBit = 0;
            }
            checkDigit = (int)checkBit;
        }
        return checkDigit;
    }

    public static boolean isValidTrailerBarcode(String code)
    {
        boolean result = false;
        if (code.length() == 10)
        {
            String firstTwoCharacters = code.substring(0,2);
            if  (firstTwoCharacters.compareToIgnoreCase("OT" ) == 0)
            {
                int checkDigit = calcCheckDigit(code);
                String lastCharacter = code.substring(code.length()-1,code.length());
                if (lastCharacter.equals(String.valueOf(checkDigit)))
                    result = true;
            }
        }
        return result;
    }

    public static boolean isValidDoorBarcode(String code)
    {
        boolean result = false;
        if (code.length() == 9)
        {
            String firstTwoCharacters = code.substring(0,2);
            if  (firstTwoCharacters.compareToIgnoreCase("OF" ) == 0)
            {
                int checkDigit = calcCheckDigit(code);
                String lastCharacter = code.substring(code.length()-1,code.length());
                if (lastCharacter.equals(String.valueOf(checkDigit)))
                    result = true;
            }
        }
        return result;
    }

    public static boolean isValidTrailerSealBarcode(String code)
    {
        boolean result = false;
        if (code.length() == 10)
        {
            String firstTwoCharacters = code.substring(0,2);
            if  (firstTwoCharacters.compareToIgnoreCase("OD" ) == 0)
            {
                int checkDigit = calcCheckDigit(code);
                String lastCharacter = code.substring(code.length()-1,code.length());
                if (lastCharacter.equals(String.valueOf(checkDigit)))
                    result = true;
            }
        }
        return result;
    }

    public static boolean isValidOnTrac2DBarcode(String code)
    {
        boolean result = false;
        if (code.substring(0,4).equals("[)>" + (char)30)){
            String splitArray[] = code.split("\035");
            if (splitArray.length >= 5)
            {
                if (splitArray[5].equals("EMSY"))
                    result = true;
            }
        }
        return result;
    }

    public static boolean isValidLaserShip2DBarcode(String code)
    {
        boolean result = false;
        String splitArray[] = code.split("\\|");
        if (splitArray.length >= 25)
        {
            if (splitArray[0].substring(0,3).toUpperCase().compareToIgnoreCase("1LS") == 0) {
                if (splitArray[1].equals("1"))
                    result = true;
            }
        }
        return result;
    }

    public static boolean ValidatePs(String code)
    {
        return code.length() == 9 && code.substring(0, 2).equals("VN");
    }

    public static boolean HasInvalidCharacter(String code) {
        boolean result = false;

        char[] array = code.toCharArray();

        for(int c: array){
            if (c < 32 | c > 130)
            {
                result = true;
                break;
            }
        }

        return result;
    }

    public static boolean ValidateOnTracCode(String code){
//        return Utilities.isValidOnTracTracking(code) | Utilities.VerifyTrackingUspsTrayLabel(code) | Utilities.VerifyTrackingUsps(code) | isValidOnTrac2DBarcode(code) | isValidLaserShipTracking(code) | isValidLaserShip2DBarcode(code);
        return Utilities.isValidOnTracTracking(code) | Utilities.VerifyTrackingUspsTrayLabel(code) | Utilities.VerifyTrackingUsps(code) | isValidLaserShipTracking(code);
    }

    public static int GetBarcodePrefixValue(char value)
    {
        return GetBarcodePrefixValue(Character.toString(value));
    }

    public static int GetBarcodePrefixValue(String value)
    {
        int result = 0;

        value = value.toUpperCase();

        switch (value)
        {
            case "2":
            case "A":
            case "K":
            case "U":
                result = 2;
                break;
            case "3":
            case "B":
            case "L":
            case "V":
                result = 3;
                break;
            case "4":
            case "C":
            case "M":
            case "W":
                result = 4;
                break;
            case "5":
            case "D":
            case "N":
            case "X":
                result = 5;
                break;
            case "6":
            case "E":
            case "O":
            case "Y":
                result = 6;
                break;
            case "7":
            case "F":
            case "P":
            case "Z":
                result = 7;
                break;
            case "8":
            case "G":
            case "Q":
                result = 8;
                break;
            case "9":
            case "H":
            case "R":
                result = 9;
                break;
            case "0":
            case "I":
            case "S":
                result = 0;
                break;
            case "1":
            case "J":
            case "T":
                result = 1;
                break;
        }
        return result;
    }

    public static boolean VerifyTrackingUsps(String code)
    {
        boolean result = false;

        int codeLength = code.length();

        if (codeLength == 31 | codeLength == 35)
        {
            if ((code.substring(0, 3).equals("420")))
            {
                int index = code.indexOf((char)29);
                if (index > 0)
                {
                    index = index + 1;
                    if ((code.substring(index, index + 1).equals("9")))
                    {
                        result = true;
                    }
                }
            }
        }
        return result;
    }

    public static boolean VerifyTrackingUspsTrayLabel(String code)
    {
        boolean result = false;

        if ((code.length() == 24))
        {
            if (Strings.IsNumber(code))
            {
                String substring1 = code.substring(8, 9);
                String substring2 = code.substring(23, 24);

                result = (substring1.equals("1") | substring1.equals("7")) & (substring2.equals("1") | substring2.equals("8"));
            }
        }

        return result;
    }

    public static String GetZipFromUspsTray(String code)
    {
        String result = "";

        if ((code.length() == 24))
        {
            if (Strings.IsNumber(code))
            {
                result = code.substring(0, 5);
            }
        }

        return result;
    }

    public static String GetZipFromUspsTrackingBarcode(String code)
    {
        String result = "";

        int codeLength = code.length();

        if (codeLength == 31 | codeLength == 35)
        {
            int index = code.indexOf((char)29);
            if (index > 0)
            {
                result = code.substring(index - 5, index);
            }
        }

        return result;
    }

    public static String GetTrackingFromUspsTrackingBarcode(String code)
    {
        String result = "";

        int codeLength = code.length();

        if (codeLength == 31 | codeLength == 35)
        {
            int index = code.indexOf((char)29);
            if (index > 0)
            {
                result = code.substring(index + 1);
            }
        }

        return result;
    }

    public static String RemoveGroupSeparator(String code)
    {
        char seperator =(char)29;

        return code.replace(String.valueOf(seperator), "");
    }

    public static String SubstituteGroupSeparator(String code)
    {
        char separator =(char)29;

        return code.replace(String.valueOf(separator), "¤");
    }

    public static String OriginalGroupSeparator(String code)
    {
        char separator =(char)29;

        return code.replace("¤", String.valueOf(separator));
    }

    public static boolean CheckStaplesCustomerReferenceNumber(String code) {
        return code.length() == 20 && Strings.IsNumber(code);
    }

    public enum CheckUspsConsolidationResult
    {
        None,
        BagForFirstClassParcelsOnly,
        FirstClassParcelsAndBelongsInFirstClassBag,
        BagForOutOfFootprintParcelsOnly,
        OutOfFootprintParcelsAndBelongsInOutOfFootprintBag,
        ParcelZipDoesNotBelongInBag,
        ZipLookupException
    }

    public static CheckUspsConsolidationResult CheckUspsConsolidation(String trayCode, String parcelCode, String trayZip, String parcelZip)
    {
        CheckUspsConsolidationResult result = CheckUspsConsolidationResult.None;
        boolean hault = false;

        String STC = parcelCode.substring(2, 5);
        String CIN = trayCode.substring(5, 8);

        List<String> lList = new ArrayList<String>();
        lList.add("001");
        lList.add("021");
        lList.add("055");
        lList.add("108");

        //First Class Parcel Test
        if (CIN.equals("292") & lList.contains(STC))
        {
            result = CheckUspsConsolidationResult.None;
            hault = true;
        }
        else if (CIN.equals("292") & !lList.contains(STC))
        {
            result = CheckUspsConsolidationResult.BagForFirstClassParcelsOnly;
            hault = true;
        }
        else if (lList.contains(STC) & !CIN.equals("292"))
        {
            result = CheckUspsConsolidationResult.FirstClassParcelsAndBelongsInFirstClassBag;
            hault = true;
        }

        //Out of Footprint Parcel Test
        if (!hault)
        {
            CheckZipResult zipCodeExist = ZipExist(parcelZip);
            if (zipCodeExist != CheckZipResult.Exception)
            {
                if (CIN.equals("664") & zipCodeExist == CheckZipResult.None)
                {
                    result = CheckUspsConsolidationResult.None;
                    hault = true;
                }
                else if (CIN.equals("664") & zipCodeExist == CheckZipResult.Match)
                {
                    result = CheckUspsConsolidationResult.BagForOutOfFootprintParcelsOnly;
                    hault = true;
                }
                else if (!CIN.equals("664") & zipCodeExist == CheckZipResult.None)
                {
                    result = CheckUspsConsolidationResult.OutOfFootprintParcelsAndBelongsInOutOfFootprintBag;
                    hault = true;
                }
            }
            else
            {
                result = CheckUspsConsolidationResult.ZipLookupException;
            }
        }

        //In Footprint Parcel Test
        if (!hault)
        {
            CheckZipResult checkZipResult = CheckZip(parcelZip, trayZip);
            if (checkZipResult != CheckZipResult.Exception)
            {
                if (checkZipResult == CheckZipResult.Match)
                {
                    result = CheckUspsConsolidationResult.None;
                    //hault = true;
                }
                else
                {
                    result = CheckUspsConsolidationResult.ParcelZipDoesNotBelongInBag;
                    //hault = true;
                }
            }
            else
            {
                result = CheckUspsConsolidationResult.ZipLookupException;
                //hault = true;
            }
        }

        return result;
    }

    public enum CheckZipResult
    {
        None,
        Match,
        Exception
    }

    public static CheckZipResult ZipExist(String zip)
    {
        CheckZipResult result = CheckZipResult.None;

        try {
            Realm realm = Realm.getDefaultInstance();
            RealmQuery<ZipCode> query = realm.where(ZipCode.class);
            final RealmResults<ZipCode> zips = query.equalTo("PackageZip", zip).findAll();
            if (zips.size() > 0) {
                result = CheckZipResult.Match;
            }
        }
        catch (Exception ex) {
            result = CheckZipResult.Exception;
        }

        return result;
    }

    public static CheckZipResult CheckZip(String uspsZip, String uspsTrayZip)
    {
        CheckZipResult result = CheckZipResult.None;

        try {
            Realm realm = Realm.getDefaultInstance();
            RealmQuery<ZipCode> query = realm.where(ZipCode.class);
            final RealmResults<ZipCode> zips = query.equalTo("PackageZip", uspsZip).findAll();
            if (zips.size() > 0) {
                String sz = zips.first().SchemeZip;
                if (uspsTrayZip.equals(sz))
                {
                    result = CheckZipResult.Match;
                }
            }
        }
        catch (Exception ex) {
            result = CheckZipResult.Exception;
        }

        return result;
    }

    /*
    public static int ImportZipCodes(APIs.ZipSchemeObject[] items){
        int recordCount = 0;

        Realm realm = Realm.getDefaultInstance();

        // delete existing data
        RealmQuery<ZipCode> query = realm.where(ZipCode.class);
        final RealmResults<ZipCode> zips = query.findAll();
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                zips.deleteAllFromRealm();
            }
        });

        realm.beginTransaction();

        for (APIs.ZipSchemeObject item: items) {
            realm.copyToRealm(new ZipCode(item));
            recordCount += 1;
        }

        realm.commitTransaction();

        if (!realm.isClosed()) {
            realm.close();
        }

        return recordCount;
    }

    public static int ImportUserInfo(APIs.UserInfoObject[] items){
        int recordCount = 0;

        Realm realm = Realm.getDefaultInstance();

        // delete existing data
        RealmQuery<User> query = realm.where(User.class);
        final RealmResults<User> zips = query.findAll();
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                zips.deleteAllFromRealm();
            }
        });

        realm.beginTransaction();

        for (APIs.UserInfoObject item: items) {
            realm.copyToRealm(new User(item));
            recordCount += 1;
        }

        realm.commitTransaction();

        if (!realm.isClosed()) {
            realm.close();
        }

        return recordCount;
    }
    */

    public static void ShowAbout(BaseApplication baseApp, Activity self){
        String asset = baseApp.onTracDeviceId.GetAssetTag();
        String user = MessageFormat.format("{0} (Id: {1}, Facility: {2})", baseApp.authUser.FriendlyName(), baseApp.authUser.Id, baseApp.authUser.FacilityCode);
        String version = BuildConfig.VERSION_NAME;
        String buildDate = new DateTime(BuildConfig.TIMESTAMP).toString();

        SharedPreferences sharedPreferences = baseApp.getSharedPreferences("SessionTimer", Context.MODE_PRIVATE);
//        String loginTime = sharedPreferences.getString("loginTime", DateTime.now().toString());

//        DateTime timeDiff = DateTime.now().minus(DateTime.parse(loginTime).toInstant().getMillis());
        Long millis = DateTime.now().getMillis() - DateTime.parse(sharedPreferences.getString("loginTime", DateTime.now().toString())).getMillis();
        int hours = (int) (millis / (1000 * 60 * 60));
        int mins = (int) ((millis / (1000 * 60)) % 60);

        String timeDiff = (hours < 10 ? "0" + hours : hours) + ":" + (mins < 10 ? "0" + mins : mins);

        BatteryManager bm = (BatteryManager)self.getSystemService(BATTERY_SERVICE);
        String battery = String.valueOf(bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)) + "%";

        String UserSync = Preferences.Get(baseApp, BaseApplication.Preference.SyncUserDate, "Never");
        String ZipSchemeSync = Preferences.Get(baseApp, BaseApplication.Preference.SyncZipSchemeDate, "Never");

        //BuildInfo.Get(baseApp);

        String html = body().attr("style", "font-size: 0.8em; padding: 0px 0px 0px 20px; margin: 0px 0px 0px 0px;").with(
                p().with(b("Time Logged In")).with(br()).withText(timeDiff.toString()),
                p().with(b("Asset Tag")).with(br()).withText(asset),
                p().with(b("User")).with(br()).withText(user),
                p().with(b("Version")).with(br()).withText(version),
                p().with(b("Build Date")).with(br()).withText(buildDate),
                p().with(b("Combined Devices")).with(br()).withText(String.valueOf(baseApp.config.Ems)),
                p().with(b("Battery")).with(br()).withText(battery),
                p().with(b("API Endpoints"))
                        .with(
                                div().attr("style", "margin: -0.5em 0px 0px 10px; font-size: 0.9em;")
                                        .with(p().with(b("User Info")).with(br()).withText(baseApp.config.ApiUserInfo))
                                        .with(p().with(b("ZIP Info")).with(br()).withText(baseApp.config.ApiZipInfo))
                                        //.with(p().with(b("ZIP Scheme")).with(br()).withText(baseApp.config.ApiZipScheme))
                                        .with(p().with(b("Scan")).with(br()).withText(baseApp.config.ApiScan))
                        ),
                //p().with(b("ZIP Scheme Synced On")).with(br()).withText(ZipSchemeSync),
                p().with(b("Users Synced On")).with(br()).withText(UserSync)
        ).toString();

        General.Alert(self, "About", html, true);
    }

    public static void LogoutAndGotoLogin(Context context){
        BaseApplication base = ((BaseApplication)context.getApplicationContext());
        base.authUser = null;

        Intent loginIntent = new Intent(base, LoginActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(loginIntent);
    }

    public static void ValidateUser(Context context, User user){
        if (user == null){
            LogoutAndGotoLogin(context);
        }
    }
}


