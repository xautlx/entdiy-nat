/**
 * Copyright @ 2020-2020 EntDIY NAT (like Ngrok) based on Netty
 *
 * Author: Li Xia, E-Mail: xautlx@hotmail.com
 *
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
package com.entdiy.nat.tester.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;

@RestController
public class TesterController {

    @GetMapping("/ping")
    public String ping() {
        return "Pong: " + LocalDateTime.now();
    }

    @GetMapping("/location")
    public void location(HttpServletRequest request, HttpServletResponse response) {
        response.setStatus(301);
        response.setHeader("Location", request.getScheme() + "://" + request.getHeader("host") + "/tester/location-to");
    }

    @GetMapping("/location-to")
    public String locationTo(HttpServletRequest request, HttpServletResponse response) {
        return "Redirect service: " + LocalDateTime.now();
    }
}
