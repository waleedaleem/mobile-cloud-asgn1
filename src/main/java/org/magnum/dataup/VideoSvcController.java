/*
 * 
 * Copyright 2014 Jules White
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
 * 
 */
package org.magnum.dataup;

import static org.magnum.dataup.VideoSvcApi.DATA_PARAMETER;
import static org.magnum.dataup.VideoSvcApi.ID_PARAMETER;
import static org.magnum.dataup.VideoSvcApi.VIDEO_DATA_PATH;
import static org.magnum.dataup.VideoSvcApi.VIDEO_SVC_PATH;
import static org.magnum.dataup.model.VideoStatus.VideoState.READY;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.FileUtils;
import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class VideoSvcController {

    public static final List<Video> videoList = new CopyOnWriteArrayList<>();
    public static final AtomicLong nextId = new AtomicLong(0);

	/**
	 * You will need to create one or more Spring controllers to fulfill the
	 * requirements of the assignment. If you use this file, please rename it
	 * to something other than "AnEmptyController"
	 * 
	 * 
		 ________  ________  ________  ________          ___       ___  ___  ________  ___  __       
		|\   ____\|\   __  \|\   __  \|\   ___ \        |\  \     |\  \|\  \|\   ____\|\  \|\  \     
		\ \  \___|\ \  \|\  \ \  \|\  \ \  \_|\ \       \ \  \    \ \  \\\  \ \  \___|\ \  \/  /|_   
		 \ \  \  __\ \  \\\  \ \  \\\  \ \  \ \\ \       \ \  \    \ \  \\\  \ \  \    \ \   ___  \  
		  \ \  \|\  \ \  \\\  \ \  \\\  \ \  \_\\ \       \ \  \____\ \  \\\  \ \  \____\ \  \\ \  \ 
		   \ \_______\ \_______\ \_______\ \_______\       \ \_______\ \_______\ \_______\ \__\\ \__\
		    \|_______|\|_______|\|_______|\|_______|        \|_______|\|_______|\|_______|\|__| \|__|
                                                                                                                                                                                                                                                                        
	 * 
	 */

    /**
     * This endpoint in the API returns a list of the videos that have been added to the server. The
     * Video objects should be returned as JSON. To manually test this endpoint, run your server and
     * open this URL in a browser: http://localhost:8080/video
     *
     * @return
     */
    @RequestMapping(value = VIDEO_SVC_PATH, method = RequestMethod.GET)
    public Collection<Video> getVideoList() {
        return videoList;
    }

    /**
     * This endpoint allows clients to add Video objects by sending POST requests that have an
     * application/json body containing the Video object information.
     *
     * @param v
     * @return
     */
    @RequestMapping(value = VIDEO_SVC_PATH, method = RequestMethod.POST)
    public Video addVideo(@RequestBody Video v) {
        v.setId(nextId.incrementAndGet());
        v.setDataUrl(String.valueOf(v.getId()));
        videoList.add(v);
        return v;
    }

    /**
     * This endpoint allows clients to set the mpeg video data for previously added Video objects by
     * sending multipart POST requests to the server. The URL that the POST requests should be sent
     * to includes the ID of the Video that the data should be associated with (e.g., replace {id}
     * in the url /video/{id}/data with a valid ID of a video, such as /video/1/data -- assuming
     * that "1" is a valid ID of a video).
     *
     * @param id
     * @param videoData
     * @return
     */
    @RequestMapping(value = VIDEO_DATA_PATH, method = RequestMethod.POST)
    public ResponseEntity<VideoStatus> setVideoData(@PathVariable(ID_PARAMETER) long id,
                                    @RequestParam(DATA_PARAMETER) MultipartFile videoData)
            throws IOException {
        if (id < 1 || id > videoList.size()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        InputStream inputStream = videoData.getInputStream();
        File targetFile = new File(String.format("src/test/resources/upload%d.mp4", id));
        FileUtils.copyInputStreamToFile(inputStream, targetFile);
        return new ResponseEntity(new VideoStatus(READY), HttpStatus.OK);
    }

    /**
     * This endpoint should return the video data that has been associated with a Video object or a
     * 404 if no video data has been set yet. The URL scheme is the same as in the method above and
     * assumes that the client knows the ID of the Video object that it would like to retrieve video
     * data for. This method uses Retrofit's @Streaming annotation to indicate that the method is
     * going to access a large stream of data (e.g., the mpeg video data on the server). The client
     * can access this stream of data by obtaining an InputStream from the Response as shown below:
     * VideoSvcApi client = ... // use retrofit to create the client Response response =
     * client.getData(someVideoId); InputStream videoDataStream = response.getBody().in();
     *
     * @param id
     * @return
     */
    @RequestMapping(value = VIDEO_DATA_PATH, method = RequestMethod.GET)
    public ResponseEntity<Resource> getData(@PathVariable(ID_PARAMETER) long id) {
        Path filePath = Paths.get("src/test/resources/").resolve(
                String.format("upload%d.mp4", id)).normalize();
        ByteArrayResource resource = null;
        try {
            resource = new ByteArrayResource(Files.readAllBytes(filePath));
        } catch (IOException e) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity(resource, HttpStatus.OK);
    }
}
