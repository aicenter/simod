/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import cz.agents.amodsim.tripUtil.StartTargetNodePair;
import java.io.IOException;

/**
 *
 * @author fido
 */
public class StartTargetNodePairSerializer extends JsonSerializer<StartTargetNodePair>{

    @Override
    public void serialize(StartTargetNodePair startTargetNodePair, JsonGenerator jg, SerializerProvider sp) 
            throws IOException, JsonProcessingException {
        jg.writeFieldName(Integer.toString(startTargetNodePair.getStartNodeId()) + "-"
                + Integer.toString(startTargetNodePair.getTargetNodeId()));
    }
    
}
