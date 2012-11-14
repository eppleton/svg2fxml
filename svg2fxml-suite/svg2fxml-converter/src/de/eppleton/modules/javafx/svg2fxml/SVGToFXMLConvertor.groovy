/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.eppleton.modules.javafx.svg2fxml

import groovy.xml.MarkupBuilder
import java.awt.geom.AffineTransform
import java.awt.geom.Point2D
/**
 *
 * @author antonepple
 */
class SVGToFXMLCOnvertor {
    def ihead ="""<?import javafx.scene.*?>
<?import javafx.scene.shape.*?>
<?import javafx.scene.paint.*?>
<?import javafx.scene.image.*?>
<?import javafx.scene.transform.*?>
<?import javafx.scene.effect.*?>
<?import javafx.scene.text.*?>"""
        
    void parseXML(String svgString){
        def svg = new XmlSlurper().parseText(svgString)
        def builder = new groovy.xml.StreamingMarkupBuilder()
        builder.encoding = "UTF-8"
        def Group = {
            mkp.xmlDeclaration()
            mkp.yieldUnescaped(ihead)
            mkp.declareNamespace("fx":"http://javafx.com/fxml")
            Group(id:"Document"){
                children(){
                    // convert everything in defs
                    fx.define(){
                        svg.defs.linearGradient.each{
                            def stopList = it.stop;       
                            def cycle = 'NO_CYCLE'
                            if (it.@spreadMethod=='reflect') cycle = 'REFLECT'
                            else if (it.@spreadMethod=='reflect') cycle = 'REPEAT'
                            def AffineTransform transform = getTransform(it)
                            def startX = it.@x1.text() as double
                            def startY = it.@y1.text() as double
                            def endX = it.@x2.text() as double
                            def endY = it.@y2.text() as double
                            def start = new Point2D.Double()
                            def end = new Point2D.Double()
                            transform.transform(new Point2D.Double(startX, startY),start)
                            transform.transform(new Point2D.Double(endX,endY),end)
                            LinearGradient("fx:id":it.@id, startX:start.x, 
                                startY:start.y,endX:end.x,endY:end.y,
                                proportional:(it.@gradientUnits=='objectBoundingBox'),
                                cycleMethod:cycle
                            ){
                                createStops(it, stopList)
                            }
                        }
                        
                    }
                }
            
            }
        }
        def writer = new FileWriter("result.fxml")
        writer << builder.bind(Group)  
    }
    
    AffineTransform getTransform(c){
        def  result = new AffineTransform()
        
        if (c.attributes().get("gradientTransform") != null){
            def gradientDef= c.attributes().get("gradientTransform").replaceAll("\\s","")
            gradientDef.split(";").each{
                if(it.startsWith("matrix(")){
                    def nums = it.substring(7,it.size()-1).split(",")
                    result.concatenate( new AffineTransform (nums[0] as double,nums[1]as double,nums[2]as double,nums[3]as double,nums[4]as double,nums[5]as double)    )
                }
                if (it.startsWith("translate(")){
                    def nums = it.substring(10,it.size()-1).split(",")
                    if (nums.size()==2){
                        result.translate(nums[0] as double,nums[1]as double)
                    }else { 
                        result.translate(nums[0] as double,0)
                    }
                }
                if (it.startsWith("scale(")){
                    def nums = it.substring(6,it.size()-1).split(",")
                    if (nums.size()==2){
                        result.scale(nums[0] as double,nums[1]as double)
                    }
                    else {
                        result.scale(nums[0] as double,nums[0]as double)
                    }
                }
                if (it.startsWith("rotate(")){
                    def nums = it.substring(7,it.size()-1).split(",")
                    if (nums.size()==3){
                        result.rotate(nums[0] as double,nums[1]as double,nums[2]as double)
                    }
                    else {
                        result.rotate(nums[0] as double)
                    }
                }
                if (it.startsWith("scewX(")){
                    def num = it.substring(6,it.size()-1)
                    result.shear(num as double,0)
                }
                if (it.startsWith("scewX(")){
                    def num = it.substring(6,it.size()-1)
                    result.shear(0,num as double)
                }
                
            }           
        }
        return result
    }
        
    void createStops(c, List stopList){
        c.stops(){                        
            stopList.each{
                def elt = it
                Stop(offset:it.@offset){                    
                    def String stopColor ='#ffffff'
                    def opacity ='1'
                    if (elt.attributes().get("style") != null){
                        elt.attributes().get("style") .split(";").each{
                            if (it.startsWith("stop-color:")){
                                stopColor = it.substring(11)
                            }
                            else if (it.startsWith("stop-opacity:")){
                                opacity = it.substring(13)
                            }
                        }
                    }
                    if (elt.attributes().get('stop-color') != null){
                        stopColor = elt.attributes().get('stop-color')
                    }
                    if (elt.attributes().get('opacity') != null){
                        opacity = elt.attributes().get('opacity')
                    }
                    color(){
                        Color(){
                            red(Integer.parseInt(stopColor.substring(1,2),16))
                            green(Integer.parseInt(stopColor.substring(3,5),16))
                            blue(Integer.parseInt(stopColor.substring(5,7),16))
                            //                                                    blue(Integer.parseInt(stopColor.substring(5,7),16))
                            //                                                    opacity(opacity)
                            //                                                    createElement(it, stopColor)
                            //                                                    createLinearGradient(it)
                        }
                    }
                }
            }
        }
    }
   
}

