package io.agora.example.utils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Utils {
    public static String[] readAppIdAndToken(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return new String[] { null, null };
        }
        String appId = null;
        String token = null;
        try {
            List<String> lines = Files.readAllLines(Paths.get(filePath));

            Pattern appIdPattern = Pattern.compile("APP_ID=(.*)");
            Pattern tokenPattern = Pattern.compile("TOKEN=(.*)");

            for (String line : lines) {
                Matcher appIdMatcher = appIdPattern.matcher(line);
                Matcher tokenMatcher = tokenPattern.matcher(line);

                if (appIdMatcher.find()) {
                    appId = appIdMatcher.group(1);
                }
                if (tokenMatcher.find()) {
                    token = tokenMatcher.group(1);
                }

                if (appId != null && token != null) {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new String[] { appId, token };
    }
}
