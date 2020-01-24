package org.egov.swService.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

@Component
public class SWConfiguration {
	
	@Value("${egov.sewerageservice.pagination.default.limit}")
	private Integer defaultLimit;

	@Value("${egov.sewerageservice.pagination.default.offset}")
	private Integer defaultOffset;
	

    @Value("${egov.idgen.scid.name}")
    private String sewerageIdGenName;

    @Value("${egov.idgen.scid.format}")
    private String sewerageIdGenFormat;
    
    @Value("${egov.idgen.scapid.name}")
    private String sewerageApplicationIdGenName;

    @Value("${egov.idgen.scapid.format}")
    private String sewerageApplicationIdGenFormat;
    
    //Idgen Config
    @Value("${egov.idgen.host}")
    private String idGenHost;

    @Value("${egov.idgen.path}")
    private String idGenPath;

}
