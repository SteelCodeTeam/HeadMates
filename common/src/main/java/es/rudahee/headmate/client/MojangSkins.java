package es.rudahee.headmate.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/** Descarga la skin pública de una cuenta de Minecraft por su nombre de usuario. */
public final class MojangSkins {
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_]{1,16}");
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public record Result(byte[] png, boolean slim) {
    }

    /** Errores que se muestran al usuario. */
    public static final class LookupException extends RuntimeException {
        public final String translationKey;

        LookupException(String translationKey) {
            super(translationKey);
            this.translationKey = translationKey;
        }
    }

    private MojangSkins() {
    }

    public static boolean isValidUsername(String name) {
        return USERNAME.matcher(name).matches();
    }

    public static CompletableFuture<Result> fetch(String username) {
        if (!isValidUsername(username)) {
            return CompletableFuture.failedFuture(new LookupException("headmate.skin.error.bad_name"));
        }
        return get("https://api.mojang.com/users/profiles/minecraft/" + URLEncoder.encode(username, StandardCharsets.UTF_8))
                .thenCompose(body -> {
                    String id = JsonParser.parseString(new String(body, StandardCharsets.UTF_8)).getAsJsonObject().get("id").getAsString();
                    return get("https://sessionserver.mojang.com/session/minecraft/profile/" + id);
                })
                .thenCompose(body -> {
                    JsonObject profile = JsonParser.parseString(new String(body, StandardCharsets.UTF_8)).getAsJsonObject();
                    String encoded = null;
                    for (var element : profile.getAsJsonArray("properties")) {
                        JsonObject property = element.getAsJsonObject();
                        if ("textures".equals(property.get("name").getAsString())) {
                            encoded = property.get("value").getAsString();
                        }
                    }
                    if (encoded == null) {
                        throw new LookupException("headmate.skin.error.no_skin");
                    }
                    JsonObject textures = JsonParser.parseString(new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8))
                            .getAsJsonObject().getAsJsonObject("textures");
                    if (textures == null || !textures.has("SKIN")) {
                        throw new LookupException("headmate.skin.error.no_skin");
                    }
                    JsonObject skin = textures.getAsJsonObject("SKIN");
                    boolean slim = skin.has("metadata") && skin.getAsJsonObject("metadata").has("model")
                            && "slim".equals(skin.getAsJsonObject("metadata").get("model").getAsString());
                    String url = skin.get("url").getAsString().replaceFirst("^http://", "https://");
                    return get(url).thenApply(png -> new Result(png, slim));
                });
    }

    private static CompletableFuture<byte[]> get(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15)).GET().build();
        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray()).thenApply(response -> {
            if (response.statusCode() == 404 || response.statusCode() == 204) {
                throw new LookupException("headmate.skin.error.not_found");
            }
            if (response.statusCode() != 200) {
                throw new LookupException("headmate.skin.error.network");
            }
            return response.body();
        });
    }
}
