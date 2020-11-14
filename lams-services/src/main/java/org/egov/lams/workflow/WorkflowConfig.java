package org.egov.lams.workflow;


import org.egov.lams.util.LRConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import java.util.Collections;
import java.util.*;



@Configuration
@PropertySource("classpath:workflow.properties")
public class WorkflowConfig {


    private Environment env;

    @Autowired
    public WorkflowConfig(Environment env) {
        this.env = env;
        setActionStatusMap();
        setRoleActionMap();
        setActionCurrentStatusMap();
    }


    private String CONFIG_ROLES ="egov.workflow.lams.roles";



    private  Map<String, String> actionStatusMap;

    private  Map<String, List<String>> roleActionMap;

    private  Map<String,  List<String>> actionCurrentStatusMap;




    private  void setActionStatusMap(){

        Map<String, String> map = new HashMap<>();

        map.put(LRConstants.ACTION_APPLY, LRConstants.STATUS_APPLIED);
        map.put(LRConstants.ACTION_APPROVE, LRConstants.STATUS_APPROVED);
        map.put(LRConstants.ACTION_REJECT, LRConstants.STATUS_REJECTED);

        actionStatusMap = Collections.unmodifiableMap(map);
    }



    private  void setRoleActionMap(){

        Map<String, List<String>> map = new HashMap<>();

        String[] keys = env.getProperty(CONFIG_ROLES).split(",");

        for(String key : keys){
            map.put(env.getProperty(key),Arrays.asList(env.getProperty(key.replace("role","action")).split(",")));
        }

        roleActionMap = Collections.unmodifiableMap(map);
    }



    private  void setActionCurrentStatusMap(){

        Map<String, List<String>> map = new HashMap<>();

        map.put(null, Arrays.asList(LRConstants.ACTION_APPLY));
        map.put(LRConstants.STATUS_APPLIED, Arrays.asList(LRConstants.ACTION_APPLY)); // FIXME PUT THE ACTIONS IN PLACE
        map.put(LRConstants.STATUS_APPROVED, Arrays.asList());
        map.put(LRConstants.STATUS_REJECTED, Arrays.asList()); // FIXME PUT THE ACTIONS IN PLACE

        actionCurrentStatusMap = Collections.unmodifiableMap(map);
    }

    public  Map<String, String> getActionStatusMap(){
        return actionStatusMap;
    }

    public  Map<String, List<String>> getActionCurrentStatusMap(){
        return actionCurrentStatusMap;
    }

    public  Map<String, List<String>> getRoleActionMap(){
        return roleActionMap;
    }



}
