package com.ontrac.warehouse.Entities;

public class Action {

    public enum ScanType {
        None,
        OS,
        RD,
        DIM,
        SD,
        AH,
        Staples,
        HW,
        PS,
        UspsOS,
        UspsRD,
        WfRD,
        WfBin,
        TrailerLoad,
        LD;
    }

    public static String GetCode(ScanType scanType){
        String result = null;

        switch (scanType) {
            case OS:
                result = "OS";
                break;
            case RD:
                result = "RD";
            break;
            case DIM:
                result = "DIM";
            break;
            case SD:
                result = "SD";
            break;
            case AH:
                result = "AH";
            break;
            case Staples:
                result = "ST";
            break;
            case HW:
                result = "HW";
            break;
            case PS:
                result = "PS";
            break;
            case UspsOS:
                result = "O2";
            break;
            case UspsRD:
                result = "R2";
            break;
            case WfRD:
                result = "WFRD";
            break;
            case WfBin:
                result = "WFBIN";
            break;
            case TrailerLoad:
                result = "TL";
                break;
            case LD:
                result = "LD";
                break;
        }

        return result;
    }

}
