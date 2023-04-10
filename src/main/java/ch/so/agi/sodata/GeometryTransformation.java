package ch.so.agi.sodata;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;

import ch.so.agi.meta2file.model.BoundingBox;

public class GeometryTransformation {
    
    // https://github.com/edigonzales-archiv/geokettle_freeframe_plugin/blob/master/src/main/java/org/catais/plugin/freeframe/FreeFrameTransformator.java
    public static Geometry convertGeometryToWGS(Geometry sourceGeometry) {
        Geometry targetGeometry = null;

        if (sourceGeometry instanceof MultiPolygon) {
            int num = sourceGeometry.getNumGeometries();
            Polygon[] polys = new Polygon[num];
            for(int j=0; j<num; j++) {
                polys[j] = transformPolygon((Polygon) sourceGeometry.getGeometryN(j));
            }    
            targetGeometry = (Geometry) new GeometryFactory().createMultiPolygon(polys);
            
        } else if (sourceGeometry instanceof Polygon) {
            targetGeometry = (Geometry) transformPolygon((Polygon) sourceGeometry);
        } else {
            targetGeometry = sourceGeometry;
        }
        return targetGeometry;
    }
    
    private static Polygon transformPolygon(Polygon p) {
        LineString shell = (LineString) p.getExteriorRing();
        LineString shellTransformed = transformLineString(shell);
        
        LinearRing[] rings = new LinearRing[p.getNumInteriorRing()];
        int num = p.getNumInteriorRing();
        for(int i=0; i<num; i++) {
            LineString line = transformLineString(p.getInteriorRingN(i));   
            rings[i] = new LinearRing(line.getCoordinateSequence(), new GeometryFactory()); 
        }               
        return new Polygon(new LinearRing(shellTransformed.getCoordinateSequence(), new GeometryFactory()), rings, new GeometryFactory());
    }

    private static LineString transformLineString(LineString l) {
        Coordinate[] coords = l.getCoordinates();
        int num = coords.length;

        Coordinate[] coordsTransformed = new Coordinate[num];
        for(int i=0; i<num; i++) {
            coordsTransformed[i] = transformCoordinate(coords[i]);
        }
        CoordinateArraySequence sequence = new CoordinateArraySequence(coordsTransformed);
        return new LineString(sequence, new GeometryFactory());
    }
    
    private static Coordinate transformCoordinate(Coordinate coord) {
        double x = ApproxSwissProj.CHtoWGSlng(coord.getX(), coord.getY());
        double y = ApproxSwissProj.CHtoWGSlat(coord.getX(), coord.getY());
        return new Coordinate(x, y);
    }

    public static BoundingBox convertBboxToWGS(BoundingBox bbox) {
        double bottom = ApproxSwissProj.CHtoWGSlat(bbox.getLeft(), bbox.getBottom());
        double left = ApproxSwissProj.CHtoWGSlng(bbox.getLeft(), bbox.getBottom());
        double top = ApproxSwissProj.CHtoWGSlat(bbox.getRight(), bbox.getTop());
        double right = ApproxSwissProj.CHtoWGSlng(bbox.getRight(), bbox.getTop());
        BoundingBox bboxWGS = new BoundingBox();
        bboxWGS.setBottom(bottom);
        bboxWGS.setLeft(left);
        bboxWGS.setTop(top);
        bboxWGS.setRight(right);
        return bboxWGS;
    }
}
