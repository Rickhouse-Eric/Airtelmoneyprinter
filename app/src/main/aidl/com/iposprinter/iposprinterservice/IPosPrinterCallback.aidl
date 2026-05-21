package com.iposprinter.iposprinterservice;
oneway interface IPosPrinterCallback {
    void onRunResult(boolean isSuccess);
    void onReturnString(String value);
}
