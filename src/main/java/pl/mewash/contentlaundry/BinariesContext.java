package pl.mewash.contentlaundry;

public class BinariesContext {
    private static String TOOLS_DIR;

    public static String getToolsDir() {
        return TOOLS_DIR;
    }

    public static void setToolsDir(String toolsDir) {
        BinariesContext.TOOLS_DIR = toolsDir;
    }
}
