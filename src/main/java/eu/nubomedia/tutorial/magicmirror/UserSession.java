/*
 * (C) Copyright 2016 NUBOMEDIA (http://www.nubomedia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package eu.nubomedia.tutorial.magicmirror;

import org.kurento.client.EventListener;
import org.kurento.client.FaceOverlayFilter;
import org.kurento.client.IceCandidate;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.OnIceCandidateEvent;
import org.kurento.client.Properties;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.jsonrpc.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.google.gson.JsonObject;

/**
 * User session.
 * 
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 6.4.1
 */
public class UserSession {

  private final Logger log = LoggerFactory.getLogger(UserSession.class);

  private final static int POINTS_PER_SESSION = 25;

  private MagicMirrorHandler handler;
  private WebRtcEndpoint webRtcEndpoint;
  private MediaPipeline mediaPipeline;
  private KurentoClient kurentoClient;
  private String sessionId;

  public UserSession(String sessionId, MagicMirrorHandler handler) {
    this.sessionId = sessionId;
    this.handler = handler;
  }

  public String startSession(final WebSocketSession session, String sdpOffer) {
    // One KurentoClient instance per session (reserving points per session)
    Properties properties = new Properties();
    properties.add("loadPoints", POINTS_PER_SESSION);
    kurentoClient = KurentoClient.create(properties);
    log.info("Created kurentoClient (session {})", sessionId);

    // Media logic (pipeline and media elements connectivity)
    mediaPipeline = kurentoClient.createMediaPipeline();
    log.info("Created Media Pipeline {} (session {})", mediaPipeline.getId(), sessionId);

    webRtcEndpoint = new WebRtcEndpoint.Builder(mediaPipeline).build();
    FaceOverlayFilter faceOverlayFilter = new FaceOverlayFilter.Builder(mediaPipeline).build();
    faceOverlayFilter.setOverlayedImage("http://files.kurento.org/img/mario-wings.png", -0.35F,
        -1.2F, 1.6F, 1.6F);
    webRtcEndpoint.connect(faceOverlayFilter);
    faceOverlayFilter.connect(webRtcEndpoint);

    // WebRTC negotiation
    webRtcEndpoint.addOnIceCandidateListener(new EventListener<OnIceCandidateEvent>() {
      @Override
      public void onEvent(OnIceCandidateEvent event) {
        JsonObject response = new JsonObject();
        response.addProperty("id", "iceCandidate");
        response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
        handler.sendMessage(session, new TextMessage(response.toString()));
      }
    });
    String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);
    webRtcEndpoint.gatherCandidates();

    return sdpAnswer;
  }

  public void addCandidate(JsonObject jsonCandidate) {
    IceCandidate candidate = new IceCandidate(jsonCandidate.get("candidate").getAsString(),
        jsonCandidate.get("sdpMid").getAsString(), jsonCandidate.get("sdpMLineIndex").getAsInt());
    webRtcEndpoint.addIceCandidate(candidate);
  }

  public void release() {
    log.info("Releasing media pipeline {} (session {})", mediaPipeline.getId(), sessionId);
    mediaPipeline.release();

    log.info("Destroying kurentoClient (session {})", sessionId);
    kurentoClient.destroy();
  }

}
