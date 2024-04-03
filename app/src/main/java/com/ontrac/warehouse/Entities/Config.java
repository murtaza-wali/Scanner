package com.ontrac.warehouse.Entities;

public class Config {
// PROD API
    public String ApiZipInfo = "https://ots.ontracshipping.net/whscapi/api/packageroutinginfo/v2/{0}";
    public String ApiZipScheme = "https://ots.ontracshipping.net/whscapi/api/zipschemezip/v1";
    public String ApiUserInfo = "https://ots.ontracshipping.net/whscapi/api/userinfo/v2";
    public String ApiScan = "https://ots.ontracshipping.net/whscapi/api/scan/v1";
    public String ApiRoutingLabel = "https://ots.ontracshipping.net/whscapi/api/packageroutinglabel/v3";
    public String ApiTrailerStatus = "https://ots.ontracshipping.net/whscapi/api/trailerstatus/v1";
    public String ApiProcessTrailer = "https://ots.ontracshipping.net/whscapi/api/processtrailer/v1";

// DEV API
//    public String ApiZipInfo = "https://ots.ontracshipping.net/whscapitest/api/packageroutinginfo/v2/{0}";
//    public String ApiZipScheme = "https://ots.ontracshipping.net/whscapitest/api/zipschemezip/v1";
//    public String ApiUserInfo = "https://ots.ontracshipping.net/whscapitest/api/userinfo/v2";
//    public String ApiScan = "https://ots.ontracshipping.net/whscapitest/api/scan/v1";
//    public String ApiRoutingLabel = "https://ots.ontracshipping.net/whscapitest/api/packageroutinglabel/v3";
//    public String ApiTrailerStatus = "https://ots.ontracshipping.net/whscapitest/api/trailerstatus/v1";
//    public String ApiProcessTrailer = "https://ots.ontracshipping.net/whscapitest/api/processtrailer/v1";

    public boolean Ems = false;
    public int ScanSyncInterval = 10;
    public int ApiConnectTimeout = 30;
    public int ApiReadTimeout = 10;
    public int ApiWriteTimeout = 10;
    public boolean LogSynced = true;

}
