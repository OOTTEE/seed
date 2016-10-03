/**
 * Copyright (c) 2013-2016, The SeedStack authors <http://seedstack.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.seedstack.seed.core.internal.crypto;

import com.google.inject.Key;
import com.google.inject.name.Names;
import org.seedstack.shed.exception.SeedException;
import org.seedstack.seed.core.utils.SeedReflectionUtils;
import org.seedstack.seed.crypto.CryptoConfig;
import org.seedstack.seed.crypto.EncryptionService;
import org.seedstack.shed.reflect.Maybe;

import javax.inject.Qualifier;
import java.lang.annotation.Annotation;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class prepare the EncryptionService bindings for Guice based on the configuration.
 *
 * @author pierre.thirouin@ext.mpsa.com (Pierre Thirouin)
 */
class EncryptionServiceBindingFactory {

    /**
     * Creates the encryption service bindings.
     *
     * @param cryptoConfig          crypto cryptoConfig
     * @param keyPairConfigurations the key pairs configurations
     * @param keyStores             the key stores instances
     * @return the map of Guice Key and EncryptionService instances.
     */
    Map<Key<EncryptionService>, EncryptionService> createBindings(CryptoConfig cryptoConfig, List<KeyPairConfig> keyPairConfigurations, Map<String, KeyStore> keyStores) {
        Map<Key<EncryptionService>, EncryptionService> encryptionServices = new HashMap<>();
        Map<String, EncryptionServiceFactory> encryptionServiceFactories = new HashMap<>();

        if (keyPairConfigurations != null && keyStores != null) {
            for (KeyPairConfig keyPairConfig : keyPairConfigurations) {
                String keyStoreName = keyPairConfig.getKeyStoreName();

                if (!encryptionServiceFactories.containsKey(keyStoreName)) {
                    EncryptionServiceFactory factory = new EncryptionServiceFactory(cryptoConfig, keyStores.get(keyStoreName));
                    encryptionServiceFactories.put(keyStoreName, factory);
                }
                EncryptionServiceFactory serviceFactory = encryptionServiceFactories.get(keyStoreName);

                EncryptionService encryptionService;
                if (keyPairConfig.getPassword() != null) {
                    encryptionService = serviceFactory.create(keyPairConfig.getAlias(), keyPairConfig.getPassword().toCharArray());
                } else {
                    encryptionService = serviceFactory.create(keyPairConfig.getAlias());
                }

                if (keyPairConfig.getQualifier() != null) {
                    encryptionServices.put(createKeyFromQualifier(keyPairConfig.getQualifier()), encryptionService);
                } else {
                    encryptionServices.put(Key.get(EncryptionService.class, Names.named(keyPairConfig.getAlias())), encryptionService);
                }
            }
        }
        return encryptionServices;
    }

    private Key<EncryptionService> createKeyFromQualifier(String qualifier) {
        Key<EncryptionService> key;
        Maybe<Class<Object>> classMaybe = SeedReflectionUtils.forName(qualifier);
        if (classMaybe.isPresent()) {
            Class<?> qualifierClass = classMaybe.get();
            if (!Annotation.class.isAssignableFrom(qualifierClass) || !qualifierClass.isAnnotationPresent(Qualifier.class)) {
                throw SeedException.createNew(CryptoErrorCode.INVALID_QUALIFIER_ANNOTATION).put("qualifier", qualifier);
            }
            //noinspection unchecked
            key = Key.get(EncryptionService.class, (Class<? extends Annotation>) qualifierClass);
        } else {
            key = Key.get(EncryptionService.class, Names.named(qualifier));
        }
        return key;
    }
}
