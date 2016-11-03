/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.testsim.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.mycompany.testsim.tripUtil.StartTargetNodePair;

/**
 *
 * @author fido
 */
public class MyModule extends SimpleModule{

    public MyModule() {
        addKeyDeserializer(StartTargetNodePair.class, new StartTargetNodePairDeserializer());
        addKeySerializer(StartTargetNodePair.class, new StartTargetNodePairSerializer());
    }
    
}
