package org.elasticsearch.index.analysis;

import org.apache.lucene.analysis.TokenStream;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.analysis.url.URLTokenFilter;
import org.elasticsearch.index.settings.IndexSettingsService;

/**
 * Joe Linn
 * 1/17/2015
 */
@AnalysisSettingsRequired
public class URLTokenFilterFactory extends AbstractTokenFilterFactory {
    private final URLPart part;
    private final boolean urlDecode;
    private final boolean allowMalformed;

    @Inject
    public URLTokenFilterFactory(Index index, IndexSettingsService indexSettings, @Assisted String name, @Assisted Settings settings) {
        super(index, indexSettings.indexSettings(), name, settings);

        this.part = URLPart.fromString(settings.get("part", "whole"));
        this.urlDecode = settings.getAsBoolean("url_decode", false);
        this.allowMalformed = settings.getAsBoolean("allow_malformed", false);
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        return new URLTokenFilter(tokenStream, part, urlDecode, allowMalformed);
    }
}
