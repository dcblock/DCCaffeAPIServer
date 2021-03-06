package com.digicap.dcblock.caffeapiserver.service.impl;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

import org.mybatis.spring.MyBatisSystemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.digicap.dcblock.caffeapiserver.dto.CategoryVo;
import com.digicap.dcblock.caffeapiserver.dto.MenuVo;
import com.digicap.dcblock.caffeapiserver.dto.MenusInCategoryDto;
import com.digicap.dcblock.caffeapiserver.exception.InvalidParameterException;
import com.digicap.dcblock.caffeapiserver.exception.NotFindException;
import com.digicap.dcblock.caffeapiserver.exception.UnknownException;
import com.digicap.dcblock.caffeapiserver.service.CategoryService;
import com.digicap.dcblock.caffeapiserver.store.CategoryMapper;
import com.digicap.dcblock.caffeapiserver.store.MenuMapper;

@Service
@Primary
@Transactional
public class CategoryServiceImpl implements CategoryService {

  private CategoryMapper categoryMapper;

  private MenuMapper menuMapper;

  @Autowired
  public CategoryServiceImpl(CategoryMapper mapper, MenuMapper menuMapper) {
    this.categoryMapper = mapper;

    this.menuMapper = menuMapper;
  }

  @Override
  public LinkedList<CategoryVo> getAllCategories() throws MyBatisSystemException, NotFindException {
    LinkedList<CategoryVo> categoriesVo = categoryMapper.selectAllCategory();
    if (categoriesVo == null || categoriesVo.size() == 0) {
      throw new NotFindException("not find resource");
    }

    return categoriesVo;
  }

  @Override
  public CategoryVo postCategory(String name) throws MyBatisSystemException {
    CategoryVo categoryVo = categoryMapper.insertCategory(name);
    return categoryVo;
  }

  @Override
  public MenusInCategoryDto deleteCategory(int code) throws MyBatisSystemException, UnknownException, NotFindException {
    MenusInCategoryDto menusInCategoryDto = new MenusInCategoryDto();

    // 카테고리 삭제하고 하위 메뉴도 삭제.
//        try {
//            if (menuMapper.existCategory(code)) {
//                // TODO extension error code
//                throw new InvalidParameterException(String.format("Code(%d) include menus.", code));
//            }
//        } catch (MyBatisSystemException e) {
//            e.printStackTrace();
//            log.error(e.getMessage());
//            throw new UnknownException(e.getMessage());
//        }

    try {
      LinkedList<MenuVo> menus = menuMapper.deleteByCategory(code);
      if (menus == null) {
        throw new UnknownException(String.format("fail delete menus by category(%d)", code));
      }

      menusInCategoryDto.setMenus(menus);

      CategoryVo categoryVo = categoryMapper.selectByCode(code);
      if (categoryVo == null) {
        throw new NotFindException(String.format("not find code(%d) in category.", code));
      }

      Integer result = categoryMapper.deleteCategory(code);
      if (result == null || result == 0) {
        throw new UnknownException(String.format("fail delete code(%d)", code));
      }

      menusInCategoryDto.setCode(categoryVo.getCode());
      menusInCategoryDto.setName(categoryVo.getName());
      menusInCategoryDto.setOrder(categoryVo.getOrder());
    } catch (NotFindException | UnknownException e) {
      throw e;
    }

    return menusInCategoryDto;
  }

  @Override
  @Transactional
  public void updateAll(LinkedList<CategoryVo> categories)
      throws MyBatisSystemException, InvalidParameterException {
    // Check.
    int size = categoryMapper.selectAllCategorySize();
    if (size != categories.size()) {
      throw new InvalidParameterException("invalid update count");
    }

    // Sort By Order.
    Collections.sort(categories, new Comparator<CategoryVo>() {

      @Override
      public int compare(CategoryVo o1, CategoryVo o2) {
        return o1.getOrder() - o2.getOrder();
      }
    });

    // Reset Order value
    for (int index = 0; index < categories.size(); index++) {
      categories.get(index).setOrder(index);
    }

    // Update.
    categoryMapper.updateCategories(categories);
  }
}
