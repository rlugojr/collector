/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.collector.inputs.file;

import com.google.inject.assistedinject.Assisted;
import com.typesafe.config.Config;
import org.graylog.collector.config.ConfigurationUtils;
import org.graylog.collector.config.constraints.IsOneOf;
import org.graylog.collector.file.GlobPathSet;
import org.graylog.collector.file.PathSet;
import org.graylog.collector.file.SinglePathSet;
import org.graylog.collector.file.splitters.ContentSplitter;
import org.graylog.collector.file.splitters.NewlineChunkSplitter;
import org.graylog.collector.file.splitters.PatternChunkSplitter;
import org.graylog.collector.inputs.InputConfiguration;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Strings.isNullOrEmpty;

@ValidFileInputConfiguration
public class FileInputConfiguration extends InputConfiguration {

    public interface Factory extends InputConfiguration.Factory<FileInputConfiguration> {
        @Override
        FileInputConfiguration create(String id, Config config);
    }

    @NotNull
    private PathSet path;

    @IsOneOf({"NEWLINE", "PATTERN"})
    private final String contentSplitter;

    @NotNull
    private final String contentSplitterPattern;

    private final FileInput.Factory inputFactory;

    @NotNull
    private final String charsetString;

    private final int readerBufferSize;
    private final long readerInterval;


    @Inject
    public FileInputConfiguration(@Assisted String id,
                                  @Assisted Config config,
                                  FileInput.Factory inputFactory) {
        super(id, config);
        this.inputFactory = inputFactory;

        if (config.hasPath("path-glob-root") && config.hasPath("path-glob-pattern")) {
            this.path = new GlobPathSet(config.getString("path-glob-root"), config.getString("path-glob-pattern"));
        } else {
            if (config.hasPath("path")) {
                this.path = new SinglePathSet(config.getString("path"));
            }
        }

        if (config.hasPath("content-splitter")) {
            this.contentSplitter = config.getString("content-splitter").toUpperCase(Locale.getDefault());

        } else {
            this.contentSplitter = "NEWLINE";
        }
        if (config.hasPath("content-splitter-pattern")) {
            this.contentSplitterPattern = config.getString("content-splitter-pattern");
        } else {
            this.contentSplitterPattern = "";
        }
        if (config.hasPath("charset")) {
            this.charsetString = config.getString("charset");
        } else {
            this.charsetString = "UTF-8";
        }
        if (config.hasPath("reader-buffer-size")) {
            this.readerBufferSize = config.getInt("reader-buffer-size");
        } else {
            this.readerBufferSize = 102400;
        }
        if (config.hasPath("reader-interval")) {
            this.readerInterval = config.getDuration("reader-interval", TimeUnit.MILLISECONDS);
        } else {
            this.readerInterval = 100L;
        }
    }

    @Override
    public FileInput createInput() {
        return inputFactory.create(this);
    }

    public PathSet getPathSet() {
        return path;
    }

    public String getContentSplitter() {
        return contentSplitter;
    }

    public String getContentSplitterPattern() {
        return contentSplitterPattern;
    }

    public ContentSplitter createContentSplitter() {
        switch (contentSplitter) {
            case "NEWLINE":
                return new NewlineChunkSplitter();
            case "PATTERN":
                return new PatternChunkSplitter(contentSplitterPattern);
            default:
                throw new IllegalArgumentException("Unknown content splitter type: " + contentSplitter);
        }
    }

    public String getCharsetString() {
        return charsetString;
    }

    public Charset getCharset() {
        return Charset.forName(charsetString);
    }

    public int getReaderBufferSize() {
        return readerBufferSize;
    }

    public long getReaderInterval() {
        return readerInterval;
    }

    @Override
    public Map<String, String> toStringValues() {
        return Collections.unmodifiableMap(new HashMap<String, String>(super.toStringValues()) {
            {
                put("path-set", getPathSet().toString());
                put("charset", getCharset().toString());
                put("content-splitter", getContentSplitter());
                if (!isNullOrEmpty(contentSplitterPattern)) {
                    put("content-splitter-pattern", getContentSplitterPattern());
                }
                put("reader-buffer-size", String.valueOf(getReaderBufferSize()));
                put("reader-interval", String.valueOf(getReaderInterval()));
            }
        });
    }

    @Override
    public String toString() {
        return ConfigurationUtils.toString(this);
    }
}
