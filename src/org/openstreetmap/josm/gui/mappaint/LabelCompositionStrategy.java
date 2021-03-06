// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.tools.LanguageInfo;

/**
 * <p>Provides an abstract parent class and three concrete sub classes for various
 * strategies on how to compose the text label which can be rendered close to a node
 * or within an area in an OSM map.</p>
 *
 * <p>The three strategies below support three rules for composing a label:
 * <ul>
 *   <li>{@link StaticLabelCompositionStrategy} - the label is given by a static text
 *   specified in the MapCSS style file</li>
 *
 *   <li>{@link TagLookupCompositionStrategy} - the label is given by the content of a
 *   tag whose name specified in the MapCSS style file</li>
 *
 *   <li>{@link DeriveLabelFromNameTagsCompositionStrategy} - the label is given by the value
 *   of one
 *   of the configured "name tags". The list of relevant name tags can be configured
 *   in the JOSM preferences
 *   content of a tag whose name specified in the MapCSS style file, see the preference
 *   option <tt>mappaint.nameOrder</tt>.</li>
 * </ul>
 * </p>
 *
 */
public abstract class LabelCompositionStrategy {

    /**
     * Replies the text value to be rendered as label for the primitive {@code primitive}.
     *
     * @param primitive the primitive
     *
     * @return the text value to be rendered or null, if primitive is null or
     * if no suitable value could be composed
     */
    abstract public String compose(OsmPrimitive primitive);

    static public class StaticLabelCompositionStrategy extends LabelCompositionStrategy {
        private String defaultLabel;

        public StaticLabelCompositionStrategy(String defaultLabel){
            this.defaultLabel = defaultLabel;
        }

        @Override
        public String compose(OsmPrimitive primitive) {
            return defaultLabel;
        }

        public String getDefaultLabel() {
            return defaultLabel;
        }

        @Override
        public String toString() {
            return "{"  + getClass().getSimpleName() + " defaultLabel=" + defaultLabel + "}";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((defaultLabel == null) ? 0 : defaultLabel.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            StaticLabelCompositionStrategy other = (StaticLabelCompositionStrategy) obj;
            if (defaultLabel == null) {
                if (other.defaultLabel != null)
                    return false;
            } else if (!defaultLabel.equals(other.defaultLabel))
                return false;
            return true;
        }
    }

    static public class TagLookupCompositionStrategy extends LabelCompositionStrategy {

        private String defaultLabelTag;
        public TagLookupCompositionStrategy(String defaultLabelTag){
            if (defaultLabelTag != null) {
                defaultLabelTag = defaultLabelTag.trim();
                if (defaultLabelTag.isEmpty()) {
                    defaultLabelTag = null;
                }
            }
            this.defaultLabelTag = defaultLabelTag;
        }

        @Override
        public String compose(OsmPrimitive primitive) {
            if (defaultLabelTag == null) return null;
            if (primitive == null) return null;
            return primitive.get(defaultLabelTag);
        }

        public String getDefaultLabelTag() {
            return defaultLabelTag;
        }

        @Override
        public String toString() {
            return "{" + getClass().getSimpleName() + " defaultLabelTag=" + defaultLabelTag + "}";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((defaultLabelTag == null) ? 0 : defaultLabelTag.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            TagLookupCompositionStrategy other = (TagLookupCompositionStrategy) obj;
            if (defaultLabelTag == null) {
                if (other.defaultLabelTag != null)
                    return false;
            } else if (!defaultLabelTag.equals(other.defaultLabelTag))
                return false;
            return true;
        }
    }

    static public class DeriveLabelFromNameTagsCompositionStrategy extends LabelCompositionStrategy {

        /**
         * The list of default name tags from which a label candidate is derived.
         */
        static public final String[] DEFAULT_NAME_TAGS = {
            "name:" + LanguageInfo.getJOSMLocaleCode(),
            "name",
            "int_name",
            "ref",
            "operator",
            "brand",
            "addr:housenumber"
        };

        private  List<String> nameTags = new ArrayList<String>();

        /**
         * <p>Creates the strategy and initializes its name tags from the preferences.</p>
         *
         * <p><strong>Note:</strong> If the list of name tags in the preferences changes, strategy instances
         * are not notified. It's up to the client to listen to preference changes and
         * invoke {@link #initNameTagsFromPreferences()} accordingly.</p>
         *
         */
        public DeriveLabelFromNameTagsCompositionStrategy() {
            initNameTagsFromPreferences();
        }

        /**
         * Sets the name tags to be looked up in order to build up the label
         *
         * @param nameTags the name tags. null values are ignore.
         */
        public void setNameTags(List<String> nameTags){
            if (nameTags == null) {
                nameTags = Collections.emptyList();
            }
            this.nameTags = new ArrayList<String>();
            for(String tag: nameTags) {
                if (tag == null) {
                    continue;
                }
                tag = tag.trim();
                if (tag.isEmpty()) {
                    continue;
                }
                this.nameTags.add(tag);
            }
        }

        /**
         * Replies an unmodifiable list of the name tags used to compose the label.
         *
         * @return the list of name tags
         */
        public List<String> getNameTags() {
            return Collections.unmodifiableList(nameTags);
        }

        /**
         * Initializes the name tags to use from a list of default name tags (see
         * {@link #DEFAULT_NAME_TAGS}) and from name tags configured in the preferences
         * using the preference key <tt>mappaint.nameOrder</tt>.
         */
        public void initNameTagsFromPreferences() {
            if (Main.pref == null){
                this.nameTags = new ArrayList<String>(Arrays.asList(DEFAULT_NAME_TAGS));
            } else {
                this.nameTags = new ArrayList<String>(
                        Main.pref.getCollection("mappaint.nameOrder", Arrays.asList(DEFAULT_NAME_TAGS))
                );
            }
        }

        private String getPrimitiveName(OsmPrimitive n) {
            String name = null;
            if (!n.hasKeys()) return null;
            for (String rn : nameTags) {
                name = n.get(rn);
                if (name != null) return name;
            }
            return null;
        }

        @Override
        public String compose(OsmPrimitive primitive) {
            if (primitive == null) return null;
            return getPrimitiveName(primitive);
        }

        @Override
        public String toString() {
            return "{" + getClass().getSimpleName() +"}";
        }
    }
}
