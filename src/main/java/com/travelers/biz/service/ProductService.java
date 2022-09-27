package com.travelers.biz.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.travelers.biz.domain.Product;
import com.travelers.biz.domain.ProductImage;
import com.travelers.biz.domain.ProductStartDate;
import com.travelers.biz.repository.ProductRepository;
import com.travelers.dto.ProductDto;
import com.travelers.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;

/**
 * @author kei
 * @since 2022-09-06
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {
    private final ProductRepository productRepository;

    public void registAll(List<ProductDto> productDtoList) {
        List<Product> productList = new ArrayList<>();
        for(ProductDto productDto: productDtoList) {
            List<ProductStartDate> productStartDates = new ArrayList<>();
            List<ProductImage> productImages = new ArrayList<>();
            Product product = Product.builder()
                    .title(productDto.getTitle())
                    .price(productDto.getPrice())
                    .thumbnail(productDto.getThumbnail())
                    .target(productDto.getTarget())
                    .destination(productDto.getDestination())
                    .theme(productDto.getTheme())
                    .priority(productDto.getPriority())
                    .summary(productDto.getSummary())
                    .packaging(productDto.getPackaging())
                    .startDates(productStartDates)
                    .images(productImages)
                    .build();
            for (Integer startDate : productDto.getStartDate() ) {
                ProductStartDate productStartDate = ProductStartDate.builder().product(product).startDate(startDate).build();
                productStartDates.add(productStartDate);
            }
            for (String image : productDto.getImage() ) {
                ProductImage productImage = ProductImage.builder().product(product).image(image).build();
                productImages.add(productImage);
            }
            productList.add(product);
        }
        productRepository.saveAll(productList);
    }

    public void loadData() throws IOException {
        List<ProductDto> productDtoList = new ArrayList<>();
        JsonArray jsonProduct = JsonUtil.getJson("json/product.json");
        for (int i = 0; i < jsonProduct.size(); i++) {
            JsonObject jsonObject = (JsonObject) jsonProduct.get(i);
            ProductDto productDto = ProductDto.builder()
                    .title(jsonObject.get("title").getAsString())
                    .price(jsonObject.get("price").getAsInt())
                    .target(jsonObject.get("target").getAsString())
                    .destination(jsonObject.get("destination").getAsString())
                    .theme(jsonObject.get("theme").getAsString())
                    .priority(jsonObject.get("priority").getAsInt())
                    .summary(jsonObject.get("summary").getAsString())
                    .packaging(jsonObject.get("packaging").getAsString())
                    .build();
            for (Object arr : jsonObject.get("startDate").getAsJsonArray()) {
                productDto.getStartDate().add(((JsonPrimitive) arr).getAsInt());
            }
            for (Object arr : jsonObject.get("image").getAsJsonArray()) {
                productDto.getImage().add(((JsonPrimitive) arr).getAsString());
            }
            productDtoList.add(productDto);
        }
        JsonArray jsonThumb = JsonUtil.getJson("json/thumbnail.json");
        for (int i = 0; i < jsonThumb.size()&& i<productDtoList.size(); i++) {
            ProductDto productDto = productDtoList.get(i);
            productDto.setThumbnail(jsonThumb.get(i).getAsString());
        }
        registAll(productDtoList);
    }

    public List<Product> getProductAll() {
        return productRepository.findAll();
    }

    public Optional<Product> getProductDetails(Long id) {
        return productRepository.findById(id);
    }
}
