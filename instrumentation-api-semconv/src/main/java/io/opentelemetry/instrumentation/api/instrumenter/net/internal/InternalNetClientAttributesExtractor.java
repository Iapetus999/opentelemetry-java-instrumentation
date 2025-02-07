/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.instrumenter.net.internal;

import static io.opentelemetry.instrumentation.api.internal.AttributesExtractorUtil.internalSet;

import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.net.NetClientAttributesGetter;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.function.BiPredicate;
import javax.annotation.Nullable;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class InternalNetClientAttributesExtractor<REQUEST, RESPONSE> {

  private final NetClientAttributesGetter<REQUEST, RESPONSE> getter;
  private final BiPredicate<Integer, REQUEST> capturePeerPortCondition;

  public InternalNetClientAttributesExtractor(
      NetClientAttributesGetter<REQUEST, RESPONSE> getter,
      BiPredicate<Integer, REQUEST> capturePeerPortCondition) {
    this.getter = getter;
    this.capturePeerPortCondition = capturePeerPortCondition;
  }

  public void onStart(AttributesBuilder attributes, REQUEST request) {
    String peerName = getter.peerName(request);
    Integer peerPort = getter.peerPort(request);

    // TODO: add host header parsing

    if (peerName != null) {
      internalSet(attributes, SemanticAttributes.NET_PEER_NAME, peerName);
      if (peerPort != null && peerPort > 0 && capturePeerPortCondition.test(peerPort, request)) {
        internalSet(attributes, SemanticAttributes.NET_PEER_PORT, (long) peerPort);
      }
    }
  }

  public void onEnd(AttributesBuilder attributes, REQUEST request, @Nullable RESPONSE response) {

    internalSet(attributes, SemanticAttributes.NET_TRANSPORT, getter.transport(request, response));

    String peerName = getter.peerName(request);
    Integer peerPort = getter.peerPort(request);

    String sockPeerAddr = getter.sockPeerAddr(request, response);
    if (sockPeerAddr != null && !sockPeerAddr.equals(peerName)) {
      internalSet(attributes, SemanticAttributes.NET_SOCK_PEER_ADDR, sockPeerAddr);

      Integer sockPeerPort = getter.sockPeerPort(request, response);
      if (sockPeerPort != null && sockPeerPort > 0 && !sockPeerPort.equals(peerPort)) {
        internalSet(attributes, SemanticAttributes.NET_SOCK_PEER_PORT, (long) sockPeerPort);
      }

      String sockFamily = getter.sockFamily(request, response);
      if (sockFamily != null && !SemanticAttributes.NetSockFamilyValues.INET.equals(sockFamily)) {
        internalSet(attributes, SemanticAttributes.NET_SOCK_FAMILY, sockFamily);
      }

      String sockPeerName = getter.sockPeerName(request, response);
      if (sockPeerName != null && !sockPeerName.equals(peerName)) {
        internalSet(attributes, SemanticAttributes.NET_SOCK_PEER_NAME, sockPeerName);
      }
    }
  }
}
