/**
 * Copyright © 2025 Jesse Gallagher
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openntf.xsp.nosql.communication.driver.keep.impl;

import static java.util.Objects.requireNonNull;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.jnosql.communication.ValueWriter;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.mapping.metadata.EntityMetadata;
import org.openntf.xsp.jakarta.nosql.communication.driver.DominoConstants;
import org.openntf.xsp.jakarta.nosql.communication.driver.impl.AbstractEntityConverter;
import org.openntf.xsp.jakarta.nosql.communication.driver.impl.EntityUtil;
import org.openntf.xsp.jakarta.nosql.mapping.extension.ItemFlags;
import org.openntf.xsp.jakarta.nosql.mapping.extension.ItemStorage;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

public class KeepEntityConverter extends AbstractEntityConverter {
  private final Jsonb jsonb;

  public KeepEntityConverter() {
    this.jsonb = JsonbBuilder.create();
  }

  @SuppressWarnings("unchecked")
  public Stream<CommunicationEntity> convertDocuments(String entityName, List<Map<String, Object>> docs,
      EntityMetadata classMapping) {
    return docs.stream()
        .map(doc -> {
          String id = getUnid(doc);

          List<Element> resultDocs = new ArrayList<>();
          resultDocs.add(Element.of(DominoConstants.FIELD_ID, id));

          doc.forEach((key, rawVal) -> {
            if("@meta".equals(key)) {
              Map<String, Object> val = (Map<String, Object>)rawVal;
              val.forEach((metaKey, metaVal) -> processMeta(metaKey, metaVal, resultDocs));
            } else if(key.startsWith("@")) {
              processMeta(key, rawVal, resultDocs);
            } else if("$FILES".equals(key)) {
              // TODO actually process
              System.out.println("see files " + rawVal);
              
              resultDocs.add(Element.of(DominoConstants.FIELD_ATTACHMENTS, Collections.emptyList()));
            } else {
              Object value;
              if(rawVal instanceof Map) {
                // Assume rich text for now
                // TODO account for encoding, MIME, etc.
                // TODO also allow split time/date values
                value = ((Map<String, String>)rawVal).get("content");
              } else {
                List<?> val = rawVal instanceof List ? (List<?>) rawVal : Arrays.asList(rawVal);
                value = val == null || val.isEmpty() ? null : val.size() == 1 ? val.get(0) : val;
              }
              if(value != null) {
                resultDocs.add(Element.of(key, value));
              }
            }
          });

          return CommunicationEntity.of(entityName, resultDocs);
        });
  }

  public Map<String, Object> convertNoSQLEntity(CommunicationEntity entity, boolean inserting,
      EntityMetadata classMapping) {
    requireNonNull(entity, "entity is required"); //$NON-NLS-1$
    List<ValueWriter<Object, Object>> writers = EntityUtil.getValueWriters();

    Map<String, Object> items = entity.elements()
        .stream()
        .filter(doc -> doc.get() != null && doc.name() != null)
        .map(doc -> {
          // TODO attachment support

          if (!DominoConstants.SKIP_WRITING_FIELDS.contains(doc.name())) {
            Optional<ItemStorage> optStorage =
                getFieldAnnotation(classMapping, doc.name(), ItemStorage.class);
            // Check if we should skip processing
            if (optStorage.isPresent()) {
              ItemStorage storage = optStorage.get();
              if (!storage.insertable() && inserting) {
                return null;
              } else if (!storage.updatable() && !inserting) {
                return null;
              }
            }

            Object value = doc.get();
            if (value == null) {
              return null;
            } else {
              Object val = value;
              for (ValueWriter<Object, Object> w : writers) {
                if (w.test(value.getClass())) {
                  val = w.write(value);
                  break;
                }
              }

              // Can't realistically handle item flags currently
              @SuppressWarnings("unused")
              Optional<ItemFlags> itemFlagsOpt =
                  getFieldAnnotation(classMapping, doc.name(), ItemFlags.class);


              // Check if the item is expected to be stored specially, which may be handled down the
              // line
              if (optStorage.isPresent() && optStorage.get().type() != ItemStorage.Type.Default) {
                ItemStorage storage = optStorage.get();
                switch (storage.type()) {
                  case JSON:
                    Object fVal = val;
                    String json = AccessController
                        .doPrivileged((PrivilegedAction<String>) () -> jsonb.toJson(fVal));
                    return Element.of(doc.name(), json);
                  case MIME:
                    throw new UnsupportedOperationException("MIME storage is unsupported");
                  case MIMEBean:
                    throw new UnsupportedOperationException("MIMEBean storage is unsupported");
                  case Default:
                  default:
                    // Shouldn't get here
                    throw new UnsupportedOperationException(
                        MessageFormat.format("Unable to handle storage type {0}", storage.type()));
                }
              } else {
                Object dominoVal = val;

                // Set number precision if applicable
                if (optStorage.isPresent()) {
                  int precision = optStorage.get().precision();
                  if (precision > 0) {
                    dominoVal = applyPrecision(dominoVal, precision);
                  }
                }

                return Element.of(doc.name(), dominoVal);
              }
            }
          }
          return null;
        })
        .filter(Objects::nonNull)
        .filter(doc -> doc.get() != null)
        .collect(Collectors.toMap(Element::name, Element::get));

    items.put(DominoConstants.FIELD_NAME, entity.name());
    return items;
  }
  
  @SuppressWarnings("unchecked")
  public String getUnid(Map<String, Object> doc) {
    if(doc.containsKey("@unid")) {
      return (String)doc.get("@unid");
    } else if(doc.containsKey("@meta")) {
      Map<String, Object> meta = (Map<String, Object>) doc.get("@meta"); //$NON-NLS-1$
      return (String)meta.get("unid");
    } else {
      return null;
    }
  }
  
  private void processMeta(String key, Object value, List<Element> resultDocs) {
    switch(String.valueOf(key)) {
    case "noteid", "@noteid" -> resultDocs.add(Element.of(DominoConstants.FIELD_NOTEID, value));
    case "lastaccessed", "@lastaccessed" -> resultDocs.add(Element.of(DominoConstants.FIELD_ADATE, OffsetDateTime.parse((String)value)));
    case "lastmodifiedinfile", "@lastmodifiedinfile" -> resultDocs.add(Element.of(DominoConstants.FIELD_MODIFIED_IN_THIS_FILE, OffsetDateTime.parse((String)value)));
    case "addedtofile", "@addedtofile" -> resultDocs.add(Element.of(DominoConstants.FIELD_ADDED, OffsetDateTime.parse((String)value)));
    case "unread", "@unread" -> resultDocs.add(Element.of(DominoConstants.FIELD_READ, !(Boolean)value));
    case "unid", "@unid" -> resultDocs.add(Element.of(DominoConstants.FIELD_ID, value));
    case "created", "@created" -> resultDocs.add(Element.of(DominoConstants.FIELD_CDATE, OffsetDateTime.parse((String)value)));
    case "lastmodified", "@lastmodified" -> resultDocs.add(Element.of(DominoConstants.FIELD_MDATE, OffsetDateTime.parse((String)value)));
    case "etag", "@etag" -> resultDocs.add(Element.of(DominoConstants.FIELD_ETAG, value));
    case "size", "@size" -> resultDocs.add(Element.of(DominoConstants.FIELD_SIZE, value));
    }
  }
}
