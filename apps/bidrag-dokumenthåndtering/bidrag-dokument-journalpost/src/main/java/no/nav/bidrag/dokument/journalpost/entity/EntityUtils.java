package no.nav.bidrag.dokument.journalpost.entity;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.util.Strings;

public class EntityUtils {

  /**
   * Columns brukerId has limited length. This method truncates userid such that the length is inside the limits
   * Supports truncating Azure application and STS serviceusers
   */
  public static String truncateBrukerId(String brukerId, Integer maxLength){
    if (Strings.isEmpty(brukerId)){
      return brukerId;
    }

    if (isSTSUser(brukerId)){
      return brukerId;
    }

    if (isNavUser(brukerId)){
      return brukerId;
    }

    return truncateAzureAppName(brukerId, maxLength);

  }

  private static String truncateAzureAppName(String brukerId, Integer maxLength){
    var processedAppName =  brukerId
        .toLowerCase()
        .replace("-", "")
        .replace("bidrag", "bid")
        .replace("dokument", "dok");

    return processedAppName.substring(0, Math.min(processedAppName.length(), maxLength));
  }

  private static boolean isNavUser(String brukerId){
    return Strings.isNotEmpty(brukerId) && brukerId.length() > 1 && NumberUtils.isCreatable(brukerId.substring(1));
  }
  private static boolean isSTSUser(String brukerId){
    return Strings.isNotEmpty(brukerId) && brukerId.startsWith("srv");
  }
}
