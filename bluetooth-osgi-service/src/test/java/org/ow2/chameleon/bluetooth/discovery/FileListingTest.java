/*
 * Copyright 2013 OW2 Chameleon
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ow2.chameleon.bluetooth.discovery;

public class FileListingTest {

    private static final String LIST_HEAD =
            "<!DOCTYPE folder-listing SYSTEM \"OBEX-folder-listing.dtd\"><folder-listing><parent-folder/>";

    private static final String LIST_TAIL =
            "</folder-listing>";

//    @Test
//    public void testRealWorldFileListing() throws FileNotFoundException, SAXException, IOException, ParserConfigurationException {
//        File listing = new File("src/test/resources/filelisting.xml");
//        List<ObexFileDescriptor> list = ObexServiceImpl.createFileDescriptors(new FileInputStream(listing));
//
//        for (ObexFileDescriptor desc : list) {
//            System.out.println(desc);
//        }
//    }
//
//    private static List<ObexFileDescriptor> createListing(final String body) throws Exception {
//        final StringBuilder buf = new StringBuilder();
//        buf.append(LIST_HEAD).append(body).append(LIST_TAIL);
//        return ObexServiceImpl.createFileDescriptors(new ByteArrayInputStream(buf.toString().getBytes()));
//    }
//
//    @Test
//    public void testAttributesPresent() throws Exception {
//        List<ObexFileDescriptor> list = createListing(
//            "<file name=\"fifo.tdu\" size=\"320\" modified=\"20110920T115531Z\" user-perm=\"RW\" />");
//
//        assertThat(list, notNullValue());
//        assertThat(list, hasSize(1));
//
//        final ObexFileDescriptor file = list.get(0);
//        assertThat(file.name, equalTo("fifo.tdu"));
//        assertThat(file.size, equalTo(320L));
//        assertThat(file.modified.get(Calendar.YEAR), equalTo(2011));
//        assertThat(file.modified.get(Calendar.MONTH), equalTo(9-1));
//        assertThat(file.modified.get(Calendar.DAY_OF_MONTH), equalTo(20));
//        assertThat(file.modified.get(Calendar.HOUR_OF_DAY), equalTo(11));
//        assertThat(file.modified.get(Calendar.MINUTE), equalTo(55));
//        assertThat(file.modified.get(Calendar.SECOND), equalTo(31));
//        assertThat(file.modified.get(Calendar.MILLISECOND), equalTo(0));
//        assertThat(file.modified.getTimeZone(), equalTo(TimeZone.getTimeZone("GMT")));
//        assertThat(file.canBeRead, is(true));
//        assertThat(file.canBeWritten, is(true));
//    }
//
//    @Test
//    public void testSkipFileIfNameIsMissing() throws Exception {
//        List<ObexFileDescriptor> list = createListing(
//            "<file size=\"320\" modified=\"20110920T115531Z\" user-perm=\"RW\" />");
//
//        assertThat(list, notNullValue());
//        assertThat(list, hasSize(0));
//    }
//
//    @Test
//    public void testEmptySizeDefaults() throws Exception {
//        List<ObexFileDescriptor> list = createListing(
//            "<file name=\"fifo.tdu\" size=\"\" modified=\"20110920T115531Z\" user-perm=\"RW\" />");
//
//        assertThat(list, notNullValue());
//        assertThat(list, hasSize(1));
//        assertThat(list.get(0).size, equalTo(0L));
//    }
//
//@Test
//    public void testMissingAttributesDefault() throws Exception {
//        List<ObexFileDescriptor> list = createListing(
//            "<file name=\"fifo.tdu\" />");
//
//        assertThat(list, notNullValue());
//        assertThat(list, hasSize(1));
//
//        final ObexFileDescriptor file = list.get(0);
//        assertThat(file.name, equalTo("fifo.tdu"));
//        assertThat(file.size, equalTo(0L));
//        assertThat(file.modified, is(nullValue()));
//        assertThat(file.canBeRead, is(true));
//        assertThat(file.canBeWritten, is(false));
//    }
}
