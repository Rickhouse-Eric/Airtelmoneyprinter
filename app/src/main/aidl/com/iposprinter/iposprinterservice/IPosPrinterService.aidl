package com.iposprinter.iposprinterservice;
import com.iposprinter.iposprinterservice.IPosPrinterCallback;
interface IPosPrinterService {
    int getPrinterStatus();
    void printerInit(in IPosPrinterCallback callback);
    void setPrinterPrintDepth(int depth, in IPosPrinterCallback callback);
    void setPrinterPrintFontType(String typeface, in IPosPrinterCallback callback);
    void setPrinterPrintFontSize(int fontsize, in IPosPrinterCallback callback);
    void setPrinterPrintAlignment(int alignment, in IPosPrinterCallback callback);
    void printerFeedLines(int lines, in IPosPrinterCallback callback);
    void printBlankLines(int lines, int height, in IPosPrinterCallback callback);
    void printText(String text, in IPosPrinterCallback callback);
    void printSpecifiedTypeText(String text, String typeface, int fontsize, in IPosPrinterCallback callback);
    void printSpecFormatText(String text, String typeface, int fontsize, int alignment, in IPosPrinterCallback callback);
    void printRawData(in byte[] rawPrintData, in IPosPrinterCallback callback);
    void printerPerformPrint(int feedlines, in IPosPrinterCallback callback);
}
