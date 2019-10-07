package com.salesmanager.shop.store.facade.manufacturer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.jsoup.helper.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.business.services.catalog.category.CategoryService;
import com.salesmanager.core.business.services.catalog.product.manufacturer.ManufacturerService;
import com.salesmanager.core.business.services.reference.language.LanguageService;
import com.salesmanager.core.model.catalog.category.Category;
import com.salesmanager.core.model.catalog.product.manufacturer.Manufacturer;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.shop.mapper.Mapper;
import com.salesmanager.shop.model.catalog.manufacturer.PersistableManufacturer;
import com.salesmanager.shop.model.catalog.manufacturer.ReadableManufacturer;
import com.salesmanager.shop.model.catalog.manufacturer.ReadableManufacturerList;
import com.salesmanager.shop.model.entity.ListCriteria;
import com.salesmanager.shop.populator.manufacturer.PersistableManufacturerPopulator;
import com.salesmanager.shop.populator.manufacturer.ReadableManufacturerPopulator;
import com.salesmanager.shop.store.api.exception.ResourceNotFoundException;
import com.salesmanager.shop.store.api.exception.ServiceRuntimeException;
import com.salesmanager.shop.store.api.exception.UnauthorizedException;
import com.salesmanager.shop.store.controller.manufacturer.facade.ManufacturerFacade;

@Service("manufacturerFacade")
public class ManufacturerFacadeImpl implements ManufacturerFacade {

  @Inject
  private Mapper<Manufacturer, ReadableManufacturer> readableManufacturerConverter;


  @Autowired
  private ManufacturerService manufacturerService;
  
  @Autowired
  private CategoryService categoryService;
  
  @Inject
  private LanguageService languageService;

  @Override
  public List<ReadableManufacturer> getByProductInCategory(MerchantStore store, Language language,
      Long categoryId) {
    Validate.notNull(store,"MerchantStore cannot be null");
    Validate.notNull(language, "Language cannot be null");
    Validate.notNull(categoryId,"Category id cannot be null");
    
    Category category = categoryService.getById(categoryId);
    
    if(category == null) {
      throw new ResourceNotFoundException("Category with id [" + categoryId + "] not found");
    }
    
    if(category.getMerchantStore().getId().longValue() != store.getId().longValue()) {
      throw new UnauthorizedException("Merchant [" + store.getCode() + "] not authorized");
    }
    
    try {
      List<Manufacturer> manufacturers = manufacturerService.listByProductsInCategory(store, category, language);
      
      List<ReadableManufacturer> manufacturersList = manufacturers.stream()
        .map(manuf -> readableManufacturerConverter.convert(manuf, store, language))
        .collect(Collectors.toList());
      
      return manufacturersList;
    } catch (ServiceException e) {
      throw new ServiceRuntimeException(e);
    }

  }

  @Override
  public void saveOrUpdateManufacturer(PersistableManufacturer manufacturer, MerchantStore store,
      Language language) throws Exception {

    PersistableManufacturerPopulator populator = new PersistableManufacturerPopulator();
    populator.setLanguageService(languageService);


    Manufacturer manuf = new Manufacturer();
    populator.populate(manufacturer, manuf, store, language);

    manufacturerService.saveOrUpdate(manuf);

    manufacturer.setId(manuf.getId());

  }

  @Override
  public void deleteManufacturer(Manufacturer manufacturer, MerchantStore store, Language language)
      throws Exception {
    manufacturerService.delete(manufacturer);

  }

  @Override
  public ReadableManufacturer getManufacturer(Long id, MerchantStore store, Language language)
      throws Exception {
    Manufacturer manufacturer = manufacturerService.getById(id);

    if (manufacturer == null) {
      return null;
    }

    ReadableManufacturer readableManufacturer = new ReadableManufacturer();

    ReadableManufacturerPopulator populator = new ReadableManufacturerPopulator();
    populator.populate(manufacturer, readableManufacturer, store, language);


    return readableManufacturer;
  }

  @Override
  public ReadableManufacturerList getAllManufacturers(MerchantStore store, Language language, ListCriteria criteria, int page, int count) {

    ReadableManufacturerList readableList = new ReadableManufacturerList();
    try {
      /**
       * Is this a pageable request
       */
      //need total count
      int total = manufacturerService.count(store);
      List<Manufacturer> manufacturers = null;
      if(page == 0 && count == 0) {
        manufacturers = manufacturerService.listByStore(store, language);
      } else {
        readableList.setRecordsTotal(total);
        //Page<Manufacturer> m = manufacturerService.listByStore(store, language, criteria.getName(), page, count);
        //manufacturers = m.getContent();
        manufacturers = manufacturerService.listByStore(store, language);
      }
      readableList.setRecordsTotal(manufacturers.size());
      
      ReadableManufacturerPopulator populator = new ReadableManufacturerPopulator();
      List<ReadableManufacturer> returnList = new ArrayList<ReadableManufacturer>();
  
      for (Manufacturer m : manufacturers) {
        ReadableManufacturer readableManufacturer = new ReadableManufacturer();
        populator.populate(m, readableManufacturer, store, language);
        returnList.add(readableManufacturer);
      }
      
       
      readableList.setManufacturers(returnList);
      return readableList;
      
    } catch (Exception e) {
      throw new ServiceRuntimeException("Error while get manufacturers",e);
    }
  }


  @Override
  public boolean manufacturerExist(MerchantStore store, String manufacturerCode) {
    Validate.notNull(store,"Store must not be null");
    Validate.notNull(manufacturerCode,"Manufacturer code must not be null");
    boolean exists = false;
    Manufacturer manufacturer = manufacturerService.getByCode(store, manufacturerCode);
    if(manufacturer!=null) {
      exists = true;
    }
    return exists;
  }


}
