insert into state.egdcr_layername(id,key,value,createdby,createddate,lastmodifiedby,lastmodifieddate,version)
select nextval('state.seq_egdcr_layername'),'LAYER_NAME_BLK_FLR_UNIT_TOILET','BLK_%s_FLR_%s_UNIT_%s_TOILET_%s',1,now(),1,now(),0 where not exists(select key from state.egdcr_layername where key='LAYER_NAME_BLK_FLR_TOILET');

insert into state.egdcr_layername(id,key,value,createdby,createddate,lastmodifiedby,lastmodifieddate,version)
select nextval('state.seq_egdcr_layername'),'LAYER_NAME_BLK_FLR_UNIT_TOILET_VENTILATION','BLK_%s_FLR_%s_UNIT_%s_TOILET_%s_VENTILATION',1,now(),1,now(),0 where not exists(select key from state.egdcr_layername where key='LAYER_NAME_BLK_FLR_TOILET_VENTILATION');

insert into state.egdcr_layername(id,key,value,createdby,createddate,lastmodifiedby,lastmodifieddate,version)
select nextval('state.seq_egdcr_layername'),'LAYER_NAME_UNIT_VERANDAH','BLK_%s_FLR_%s_UNIT_%s_VERANDAH',1,now(),1,now(),0 where not exists(select key from state.egdcr_layername where key='LAYER_NAME_VERANDAH');
