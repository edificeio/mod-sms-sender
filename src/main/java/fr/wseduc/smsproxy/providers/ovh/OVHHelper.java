/*
 * Copyright © WebServices pour l'Éducation, 2014
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

package fr.wseduc.smsproxy.providers.ovh;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Map.Entry;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.json.JsonObject;

public class OVHHelper {
	
	private final static String API_VERSION = "1.0";
	
	public enum OVH_ENDPOINT{
		// ovh_eu: OVH Europe
		ovh_eu("eu.api.ovh.com"),
		// ovh_ca: OVH North America
		ovh_ca("ca.api.ovh.com"),
		// runabove_ca: RunAbove
		runabove_ca("api.runabove.com"),
		// sys_eu: SoYouStart Europe
		sys_eu("eu.api.soyoustart.com"),
		// sys_ca: SoYouStart North America
		sys_ca("ca.api.soyoustart.com"),
		// ks_eu: Kimsufi Europe
		ks_eu("eu.api.kimsufi.com"),
		// ks_ca: Kimsufi North America
		ks_ca("ca.api.kimsufi.com");
		
		private String val;
		
		private OVH_ENDPOINT(String s){
			this.val = s;
		}
		public String getValue(){
			return this.val;
		}
	};

	public static String getRequestSignature(String AS, String CK, String method, String query, String body, String timestamp) throws NoSuchAlgorithmException{
		MessageDigest md = MessageDigest.getInstance("SHA1");
		byte[] result = md.digest((AS+"+"+CK+"+"+method+"+"+query+"+"+body+"+"+timestamp).getBytes());
		
		StringBuffer sb = new StringBuffer("$1$");
        for (int i = 0; i < result.length; i++) {
            sb.append(Integer.toString((result[i] & 0xff) + 0x100, 16).substring(1));
        }
		
		return sb.toString();
	}
	
	public static class OVHClient{
		
		private HttpClient httpclient;
		private Long timeDiff = null;
		private String AK, AS, CK, endPoint;
		
		public OVHClient(Vertx vertx, String endPoint, String AK, String AS, String CK){
			this.AK = AK;
			this.AS = AS;
			this.CK = CK;
			this.endPoint = endPoint;
			HttpClientOptions options = new HttpClientOptions()
					.setDefaultHost(endPoint).setSsl(true).setDefaultPort(443);
			this.httpclient = vertx.createHttpClient(options);
		}
		
		private void getOvhTime(final Handler<Long> handler){
			httpclient.request(new RequestOptions().setMethod(HttpMethod.GET).setURI("/" + API_VERSION + "/auth/time"))
					.flatMap(HttpClientRequest::send)
					.onSuccess(response -> {
						if(response.statusCode() == 200){
							response.bodyHandler(new Handler<Buffer>() {
								public void handle(Buffer body) {
									handler.handle(Long.parseLong(body.getString(0, body.length())));
								}
							});
						} else {
							handler.handle(new Date().getTime() / 1000l);
						}
					});
		}
		
		private void request(final String httpMethod, final String basepath, final JsonObject params, final Handler<HttpClientResponse> handler){
			
			//First time - retrieving ovh timestamp and adjusting to the server time.
			if(timeDiff == null){
				this.getOvhTime(new Handler<Long>() {
					public void handle(Long ovhTime) {
						long serverTime = Math.round(new Date().getTime() / 1000l);
						timeDiff = ovhTime - serverTime;
						request(httpMethod, basepath, params, handler);
					}
				});
				return;
			}
			
			//Prepend API version
			final String path = "/" + API_VERSION + basepath;
			
			//Append query parameters
			StringBuilder query = new StringBuilder();
			String fullPath = path;
			switch(httpMethod){
				case "GET":
				case "DELETE":
					for(Entry<String, Object> entry : params.getMap().entrySet()){
						if(query.length() == 0){
							query.append("?");
						} else {
							query.append("&");
						}
						query.append(entry.getKey()).append("=").append(entry.getValue());
					}
					fullPath += query.toString();
					break;
				default:
			}
			
			//Create body
			StringBuilder body = new StringBuilder();
			switch(httpMethod){
				case "POST":
				case "PUT":
					body.append(params.toString());
					break;
				default:
			}
			
			String timestamp = Long.toString(Math.round(new Date().getTime() / 1000l) + timeDiff);
			
			//Fill headers
			HeadersMultiMap headers = new HeadersMultiMap();
			headers.add("Content-Type", "application/json");
			//OVH specific fields
			headers.add("X-Ovh-Application", AK);
			headers.add("X-Ovh-Consumer", CK);
			headers.add("X-Ovh-Timestamp", Long.toString(Math.round(new Date().getTime() / 1000l) + timeDiff));
			try{
				headers.add("X-Ovh-Signature", getRequestSignature(AS, CK, httpMethod, "https://"+endPoint+fullPath, body.toString(), timestamp));
			} catch(NoSuchAlgorithmException e){
				//TODO - better error logging.
				e.printStackTrace();
			}

			//Fill body
			Buffer bodyBuffer = Buffer.buffer();
			if(body.length() > 0){
				bodyBuffer.appendString(body.toString(), "UTF-8");
				headers.add("Content-Length", Integer.toString(bodyBuffer.length()));
				httpclient.request(new RequestOptions()
								.setMethod(HttpMethod.valueOf(httpMethod))
								.setURI(fullPath)
								.setHeaders(headers))
						.flatMap(request -> request.send(bodyBuffer));
			} else {
				httpclient.request(new RequestOptions()
								.setMethod(HttpMethod.valueOf(httpMethod))
								.setURI(fullPath)
								.setHeaders(headers))
						.flatMap(HttpClientRequest::send);
			}
		}
		
		public void get(final String path, final JsonObject params, final Handler<HttpClientResponse> handler){
			request("GET", path, params, handler);
		}
		public void post(final String path, final JsonObject params, final Handler<HttpClientResponse> handler){
			request("POST", path, params, handler);
		}
		public void put(final String path, final JsonObject params, final Handler<HttpClientResponse> handler){
			request("PUT", path, params, handler);
		}
		public void delete(final String path, final JsonObject params, final Handler<HttpClientResponse> handler){
			request("DELETE", path, params, handler);
		}
		
	}
	
}
