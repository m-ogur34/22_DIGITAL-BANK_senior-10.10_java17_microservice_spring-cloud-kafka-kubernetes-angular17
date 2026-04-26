package com.digitalbank.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Tüm servislerin döndüğü standart API yanıt wrapper'ı.
 *
 * Neden generic? T parametresi sayesinde her türlü veriyi taşıyabilir:
 * ApiResponse<UserDto>, ApiResponse<List<AccountDto>>, ApiResponse<Void> gibi.
 *
 * @JsonInclude(NON_NULL): null alanlar JSON çıktısına dahil edilmez,
 * böylece istemci tarafında belirsizlik olmaz ve yanıt boyutu küçülür.
 *
 * Örnek başarılı yanıt:
 * {
 *   "success": true,
 *   "message": "İşlem başarılı",
 *   "data": { "id": "abc", "balance": "1500.00" },
 *   "timestamp": "2024-01-15T10:30:00"
 * }
 *
 * Örnek hata yanıtı:
 * {
 *   "success": false,
 *   "message": "Yetersiz bakiye",
 *   "errorCode": "INSUFFICIENT_FUNDS",
 *   "timestamp": "2024-01-15T10:30:00"
 * }
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    // İşlemin başarılı mı yoksa başarısız mı olduğunu belirtir
    private final boolean success;

    // İnsan tarafından okunabilir açıklama mesajı
    private final String message;

    // Başarılı yanıtta dönen veri; hata durumunda null
    private final T data;

    // Hata durumunda makine tarafından işlenebilir hata kodu
    private final String errorCode;

    // Yanıtın oluşturulduğu zaman damgası
    @Builder.Default
    private final LocalDateTime timestamp = LocalDateTime.now();

    // ===== Factory metodlar =====

    /**
     * Başarılı yanıt oluşturmak için kısayol metod.
     * Kullanım: ApiResponse.success("Hesap oluşturuldu", accountDto)
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Veri olmadan başarılı yanıt (örn. silme işlemi).
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .build();
    }

    /**
     * Hata yanıtı oluşturmak için kısayol metod.
     * Kullanım: ApiResponse.error("Yetersiz bakiye", "INSUFFICIENT_FUNDS")
     */
    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .build();
    }
}
