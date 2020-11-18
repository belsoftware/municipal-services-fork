package org.egov.lams.repository.builder;

import java.util.List;

import org.egov.lams.config.LamsConfiguration;
import org.egov.lams.model.SearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LamsQueryBuilderMaster {

    private LamsConfiguration config;

    @Autowired
    public LamsQueryBuilderMaster(LamsConfiguration config) {
        this.config = config;
    }

    private static final String LEFT_JOIN = " LEFT JOIN ";

    private static final String QUERY_Master = "SELECT survey.* ,mst.* ,loc.* , " 
    		+ "survey.id as survey_id ,mst.id as mst_id, loc.id as loc_id " 
    		+ "FROM public.eg_lams_survey_no_details survey" 
    		+ LEFT_JOIN
    		+ "eg_lams_mst_office mst on mst.id=survey.mstofficeid " 
    		+ LEFT_JOIN
    		+ "eg_lams_property_location loc on loc.id=survey.propertylocationid";

      private  String paginationWrapperMaster = "SELECT * FROM " +
              "(SELECT *, DENSE_RANK() OVER (ORDER BY survey_id ) offset_ FROM " +
              "({})" +
              " result) result_offset " +
              "WHERE offset_ > ? AND offset_ <= ?";



    private String addPaginationWrapper(String query,List<Object> preparedStmtList,
                                      SearchCriteria criteria ){
        int limit = config.getDefaulLamsimit();
        int offset = config.getDefaultOffset();
        String finalQuery = paginationWrapperMaster.replace("{}",query);

        if(criteria.getLimit()!=null && criteria.getLimit()<=config.getMaxSearchLimit())
            limit = criteria.getLimit();

        if(criteria.getLimit()!=null && criteria.getLimit()>config.getMaxSearchLimit())
            limit = config.getMaxSearchLimit();

        if(criteria.getOffset()!=null)
            offset = criteria.getOffset();

        preparedStmtList.add(offset);
        preparedStmtList.add(limit+offset);

       return finalQuery;
    }


    private static void addClauseIfRequired(List<Object> values, StringBuilder queryString) {
        if (values.isEmpty())
            queryString.append(" WHERE ");
        else {
            queryString.append(" AND");
        }
    }


	public String getLeaseDetails(SearchCriteria criteria, List<Object> preparedStmtList) {
		StringBuilder builder = new StringBuilder(QUERY_Master);

		if (criteria.getTenantId() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" mst.tenantid=? ");
			preparedStmtList.add(criteria.getTenantId());
		}
		if (criteria.getLocated() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" loc.location=? ");
			preparedStmtList.add(criteria.getLocated());
		}
		if (criteria.getSurveyNo()!= null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append("  survey.surveyno = ? ");
			preparedStmtList.add(criteria.getSurveyNo());
		}
		return addPaginationWrapper(builder.toString(), preparedStmtList, criteria );
	}

}
