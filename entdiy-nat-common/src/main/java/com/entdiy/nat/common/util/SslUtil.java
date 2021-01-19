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
package com.entdiy.nat.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;

@Slf4j
public class SslUtil {
    public static TrustManager[] getTrustManagersServer(String jksFileName, String keyStorePass) {
        FileInputStream is = null;
        TrustManager[] trustManagersw = null;
        TrustManagerFactory trustManagerFactory = null;
        KeyStore ks = null;
        try {
            trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
            is = is = new FileInputStream((new ClassPathResource(jksFileName)).getFile());
            ks = KeyStore.getInstance("JKS");
            ks.load(is, keyStorePass.toCharArray());
            trustManagerFactory.init(ks);
            trustManagersw = trustManagerFactory.getTrustManagers();
        } catch (Exception e) {
            log.error("SSL Error", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    log.error("SSL Error", e);
                }
            }
        }
        return trustManagersw;
    }

    public static KeyManager[] getKeyManagersServer(String jksFileName, String keyStorePass) {
        FileInputStream is = null;
        KeyStore ks = null;
        KeyManagerFactory keyFac = null;

        KeyManager[] kms = null;
        try {
            keyFac = KeyManagerFactory.getInstance("SunX509");
            is = new FileInputStream((new ClassPathResource(jksFileName)).getFile());
            ks = KeyStore.getInstance("JKS");
            ks.load(is, keyStorePass.toCharArray());
            keyFac.init(ks, keyStorePass.toCharArray());
            kms = keyFac.getKeyManagers();
        } catch (Exception e) {
            log.error("SSL Error", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    log.error("SSL Error", e);
                }
            }
        }
        return kms;
    }
}
