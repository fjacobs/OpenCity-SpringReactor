/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dynacore.livemap.traveltime.service;

import com.dynacore.livemap.core.adapter.GeoJsonAdapter;
import com.dynacore.livemap.core.service.GeoJsonReactorService;
import com.dynacore.livemap.core.service.TrafficFeatureDistinct;
import com.dynacore.livemap.traveltime.domain.TravelTimeDTO;
import com.dynacore.livemap.traveltime.domain.TravelTimeFeature;
import com.dynacore.livemap.traveltime.repo.TravelTimeEntity;
import com.dynacore.livemap.traveltime.repo.TravelTimeRepo;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Road traffic traveltime service
 *
 * <p>This service will subscribe to a traffic information data source. It checks the GeoJson for
 * RFC compliance and calculates properties and publishes the new data in a reactive manner. The
 * data is automatically saved in a reactive database (R2DBC) and the last emitted signals are
 * cached for new subscribers.
 */
@Lazy(false)
@Profile("traveltime")
@Service("travelTimeService")
public class TravelTimeService
    extends GeoJsonReactorService<TravelTimeEntity, TravelTimeFeature, TravelTimeDTO> {

  Logger log = LoggerFactory.getLogger(TravelTimeService.class);

  public TravelTimeService(
      TravelTimeServiceConfig config,
      GeoJsonAdapter adapter,
      TravelTimeImporter importer,
      TravelTimeRepo repo,
      TravelTimeEntityDistinct entityDtoDistinct,
      TravelTimeFeatureDistinct featureDistinct
      )
      throws JsonProcessingException {
    super(config, adapter, importer, repo,  entityDtoDistinct, featureDistinct);

    if (config.isSaveToDbEnabled()) {
      Flux.from(importedFlux)
          .parallel(Runtime.getRuntime().availableProcessors())
          .runOn(Schedulers.parallel())
          .map(feature -> repo.save(new TravelTimeEntity(feature)))
          .subscribe(Mono::subscribe, error -> log.error("Error: " + error));
    }
  }
}
