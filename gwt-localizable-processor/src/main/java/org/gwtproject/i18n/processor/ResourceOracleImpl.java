/*
 *
 * Copyright © ${year} ${name}
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
package org.gwtproject.i18n.processor;

import com.google.auto.common.MoreElements;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.processing.FilerException;
import javax.lang.model.element.ExecutableElement;
import javax.tools.FileObject;
import javax.tools.JavaFileManager.Location;
import javax.tools.StandardLocation;
import org.gwtproject.ext.UnableToCompleteException;

/** @author Dmitrii Tikhomirov chani.liet@gmail.com Created by treblereel on 10/8/18. */
public class ResourceOracleImpl implements ResourceOracle {
  private final AptContext aptContext;

  public ResourceOracleImpl(AptContext context) {
    this.aptContext = context;
  }

  private URL[] getResourcesByExtensions(ExecutableElement method, String[] extensions)
      throws UnableToCompleteException {
    String[] paths = new String[extensions.length];
    for (int i = 0; i < extensions.length; i++) {
      StringBuffer sb = new StringBuffer();
      sb.append(method.getSimpleName().toString()).append(extensions[i]);
      paths[i] = sb.toString();
    }
    return findResources(MoreElements.getPackage(method).getQualifiedName().toString(), paths);
  }

  @Override
  public URL[] findResources(CharSequence packageName, CharSequence[] pathName) {
    List<URL> result = new ArrayList<>();
    for (int i = 0; i < pathName.length; i++) {
      URL resource = findResource(packageName, pathName[i]);
      if (resource != null) {
        result.add(resource);
      } else {
        resource = findResource(pathName[i]);
        if (resource != null) {
          result.add(resource);
        }
      }
    }
    if (result.size() > 0) {
      return result.toArray(new URL[result.size()]);
    }
    return null;
  }

  /**
   * Locates a resource by searching multiple locations.
   *
   * <p>This method assumes that the path is a full package path such as <code>
   * org/gwtproject/uibinder/example/view/SimpleFormView.ui.xml</code>
   *
   * @return URL or null if file is not found.
   * @see #findResource(CharSequence, CharSequence)
   */
  @Override
  public URL findResource(CharSequence path) {
    String packageName = "";
    String relativeName = path.toString();

    int index = relativeName.lastIndexOf('/');
    if (index >= 0) {
      packageName = relativeName.substring(0, index).replace('/', '.');
      relativeName = relativeName.substring(index + 1);
    }

    return findResource(packageName, relativeName);
  }

  /**
   * Locates a resource by searching multiple locations.
   *
   * <p>Searches in the order of
   *
   * <ul>
   *   <li>{@link StandardLocation#SOURCE_PATH}
   *   <li>{@link StandardLocation#SOURCE_OUTPUT}
   *   <li>{@link StandardLocation#CLASS_PATH}
   *   <li>{@link StandardLocation#CLASS_OUTPUT}
   *   <li>{@link StandardLocation#ANNOTATION_PROCESSOR_PATH}
   * </ul>
   *
   * @return FileObject or null if file is not found.
   */
  @Override
  public URL findResource(CharSequence pkg, CharSequence relativeName) {
    return findResource(
        Arrays.asList(
            StandardLocation.SOURCE_PATH,
            StandardLocation.SOURCE_OUTPUT,
            StandardLocation.CLASS_PATH,
            StandardLocation.CLASS_OUTPUT,
            StandardLocation.ANNOTATION_PROCESSOR_PATH),
        pkg,
        relativeName);
  }

  /**
   * Locates a resource by searching multiple locations.
   *
   * @return FileObject or null if file is not found in given locations.
   */
  private URL findResource(
      List<Location> searchLocations, CharSequence pkg, CharSequence relativeName) {
    for (Location location : searchLocations) {
      String path = "";
      if (pkg.length() > 0) {
        path = String.valueOf(pkg).replace('.', File.separatorChar) + '/';
      }
      try {
        FileObject fileObject = aptContext.filer.getResource(location, "", path + relativeName);
        if (new File(fileObject.getName()).exists()) {
          return fileObject.toUri().toURL();
        }
      } catch (FilerException ignored) {
        File openedfile =
            new File(ignored.getMessage().replace("Attempt to reopen a file for path ", ""));
        if (openedfile.exists()) {
          try {
            return openedfile.toURI().toURL();
          } catch (MalformedURLException e) {
            // ignored
          }
        }
        // ignored
      } catch (IOException ignored) {
        // ignored
      }
    }
    // unable to locate, return null.
    return null;
  }
}
