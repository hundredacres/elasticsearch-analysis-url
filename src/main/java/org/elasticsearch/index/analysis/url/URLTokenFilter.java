package org.elasticsearch.index.analysis.url;

import com.google.common.base.Strings;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.elasticsearch.index.analysis.URLPart;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Joe Linn
 * 1/17/2015
 */
public final class URLTokenFilter extends TokenFilter {
    public static final String NAME = "url";

    private final URLPart part;

    private final boolean urlDeocde;

    private final CharTermAttribute termAttribute = addAttribute(CharTermAttribute.class);

    private final boolean allowMalformed;

    private boolean parsed;

    public URLTokenFilter(TokenStream input, URLPart part) {
        this(input, part, false);
    }

    public URLTokenFilter(TokenStream input, URLPart part, boolean urlDecode) {
        this(input, part, urlDecode, false);
    }

    public URLTokenFilter(TokenStream input, URLPart part, boolean urlDecode, boolean allowMalformed) {
        super(input);
        this.part = part;
        this.urlDeocde = urlDecode;
        this.allowMalformed = allowMalformed;
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (input.incrementToken() && !parsed) {
            final String urlString = termAttribute.toString();
            termAttribute.setEmpty();
            if (Strings.isNullOrEmpty(urlString) || urlString.equals("null")) {
                return false;
            }
            String partString;
            try {
                URL url = new URL(urlString);
                partString = URLUtils.getPart(url, part);
                parsed = !Strings.isNullOrEmpty(partString);
            } catch (MalformedURLException e) {
                if (allowMalformed) {
                    partString = parseMalformed(urlString);
                    if (Strings.isNullOrEmpty(partString)) {
                        return false;
                    }
                    parsed = true;
                } else {
                    throw e;
                }
            }
            if (urlDeocde) {
                partString = URLDecoder.decode(partString, "UTF-8");
            }
            termAttribute.append(partString);
            return true;
        }
        return false;
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        parsed = false;
    }

    private static final Pattern REGEX_PROTOCOL = Pattern.compile("^([a-zA-Z]+)(?=://)");
    private static final Pattern REGEX_PORT = Pattern.compile(":([0-9]{1,5})");
    private static final Pattern REGEX_QUERY = Pattern.compile("\\?(.+)");

    /**
     * Attempt to parse a malformed url string
     * @param urlString the malformed url string
     * @return the url part if it can be parsed, null otherwise
     */
    private String parseMalformed(String urlString) {
        switch (part) {
            case PROTOCOL:
                return applyPattern(REGEX_PROTOCOL, urlString);
            case PORT:
                return applyPattern(REGEX_PORT, urlString);
            case QUERY:
                return applyPattern(REGEX_QUERY, urlString);
            case WHOLE:
                return urlString;
            default:
                return null;
        }
    }

    /**
     * Apply the given regex pattern to the given malformed url string and return the first match
     * @param pattern the pattern to match
     * @param urlString the malformed url to which the pattern should be applied
     * @return the first match if one exists, null otherwise
     */
    private String applyPattern(Pattern pattern, String urlString) {
        Matcher matcher = pattern.matcher(urlString);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
