import site
import polyglot
import os
import math
import re
import pystac
import json
from datetime import date, datetime, timezone, timedelta

class StacCreator:
    def create(self, collection_file_path, theme_publication, root_url):
        print("StacCreator.create()")

        # Collection
        collection_id = theme_publication.getIdentifier()
        collection_title = theme_publication.getTitle()
        collection_description = theme_publication.getShortDescription()
        collection_licence = str(theme_publication.getLicence())
        collection_bbox = theme_publication.getBbox()

        spatial_extent = pystac.SpatialExtent(bboxes=[[collection_bbox.getLeft(), 
                                    collection_bbox.getBottom(),
                                    collection_bbox.getRight(),
                                    collection_bbox.getTop()]])

        # collection_interval = sorted([
        #                             datetime.fromisoformat(theme_publication.getSecondToLastPublishingDate().toString()).replace(tzinfo=timezone(timedelta(hours=2))), 
        #                             datetime.fromisoformat(theme_publication.getLastPublishingDate().toString()).replace(tzinfo=timezone(timedelta(hours=2)))
        #                             ]) 

        # Umweg via date notwendig, da ein Java-(Local-)Date-Objekt nicht gemappt wird. Es bleibt 'foreign'.
        # replace(tzinfo=...) funktioniert nicht mit date, sondern nur mit datetime.

        collection_last_publishing_date = datetime.fromisoformat(theme_publication.getLastPublishingDate().toString()).replace(tzinfo=timezone(timedelta(hours=2)))
        collection_interval = [None, collection_last_publishing_date]

        temporal_extent = pystac.TemporalExtent(intervals=[collection_interval])
        collection_extent = pystac.Extent(spatial=spatial_extent, temporal=temporal_extent)

        collection = pystac.Collection(id=collection_id,
                                    title=collection_title,
                                    description=collection_description,
                                    extent=collection_extent,
                                    license=collection_licence)

        # Item(s)
        for itemDTO in theme_publication.getItems():
            item_extent = [collection_bbox.getLeft(),collection_bbox.getBottom(),collection_bbox.getRight(),collection_bbox.getTop()],

            if len(theme_publication.getItems()) == 1:
                item_id = collection_id
            else:
                item_id = itemDTO.getIdentifier() + "." + collection_id

            item = pystac.Item(id=item_id,
                 geometry=json.loads(itemDTO.getGeometry()),
                 bbox=item_extent[0], # Verstehe ich nicht.
                 datetime=collection_last_publishing_date,
                 properties={})

            #item.set_self_href(href=(root_url + collection_id + "/" + item_id + ".json"))

            # Asset(s)
            for fileFormatDTO in theme_publication.getFileFormats():
                file_abbreviation = fileFormatDTO.getAbbreviation()
                # file_ext = "zip"
                # if theme_publication.getModel() == None:
                #     file_ext = file_abbreviation

                files_server_url = str(theme_publication.getDownloadHostUrl())
                if len(theme_publication.getItems()) == 1:
                    asset_url = files_server_url + "/" + collection_id + "/aktuell/" + collection_id + "." + file_abbreviation # + "." + file_ext
                else:
                    asset_url = files_server_url + "/" + collection_id + "/aktuell/" + item_id + "." + file_abbreviation # + "." + file_ext

                asset=pystac.Asset(
                    href=asset_url,
                    title= itemDTO.getTitle() + " (" + file_abbreviation + ")",
                    media_type=fileFormatDTO.getMimetype()
                )
                
                if len(theme_publication.getItems()) == 1:
                    key = collection_id + "." + file_abbreviation
                else:
                    key = item_id + "." + collection_id + "." + file_abbreviation

                item.add_asset(key=key, asset=asset)

            #item.validate()
            
            collection.add_item(item)

        # https://www.jsonschemaval§idator.net/ 
        # Gemäss Online-Validator sind die JSON-Dateien korrekt ggü collection- und item Schema. 
        #collection.validate_all()

        # Save everything to disk
        #collection.normalize_and_save(root_href=os.path.join(collection_file_path, collection_id), catalog_type=pystac.CatalogType.SELF_CONTAINED)

        #collection.set_self_href(href=(root_url + collection_id + "/" + "collection.json"))
        #collection.normalize_hrefs(root_href=(root_url + collection_id))         
        #collection.save(catalog_type=pystac.CatalogType.ABSOLUTE_PUBLISHED, dest_href=os.path.join(collection_file_path, collection_id))

        collection.normalize_hrefs(root_href=(root_url + collection_id))
        collection.save(catalog_type=pystac.CatalogType.SELF_CONTAINED, dest_href=os.path.join(collection_file_path, collection_id))

    def create_catalog(self, collection_file_path, collections, root_url):
        print("StacCreator.create_catalog()")

        catalog = pystac.Catalog(id='ch.so.geo', description='Geodaten Kanton Solothurn.')

        for collection_id in collections:
            print("<collection_id> " + collection_id)
            collection = pystac.Collection.from_file(os.path.join(collection_file_path,collection_id, "collection.json"))
            catalog.add_child(child=collection)
        
        catalog.normalize_hrefs(root_href=root_url);      
        #catalog.save(catalog_type=pystac.CatalogType.SELF_CONTAINED, dest_href=os.path.join(collection_file_path)) 

        catalog.set_self_href(href=root_url + "catalog.json")
        catalog.save(catalog_type=pystac.CatalogType.ABSOLUTE_PUBLISHED, dest_href=os.path.join(collection_file_path))

polyglot.export_value("StacCreator", StacCreator)
