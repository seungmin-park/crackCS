package com.example.crackcs.evaluation.adapter.openai;

import com.example.crackcs.exception.ProviderRequestRejectedException;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

final class BoundedResponseBodySubscriber implements HttpResponse.BodySubscriber<String> {
    private final int maxResponseBytes;
    private final long declaredLength;
    private final ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
    private final CompletableFuture<String> result = new CompletableFuture<>();
    private final byte[] copyBuffer;
    private Flow.Subscription subscription;
    private int receivedBytes;

    BoundedResponseBodySubscriber(int maxResponseBytes, long declaredLength) {
        this.maxResponseBytes = maxResponseBytes;
        this.declaredLength = declaredLength;
        this.copyBuffer = new byte[Math.min(maxResponseBytes, 8192)];
    }

    @Override
    public CompletionStage<String> getBody() {
        return result;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        if (this.subscription != null) {
            subscription.cancel();
            return;
        }
        this.subscription = subscription;
        if (declaredLength > maxResponseBytes) {
            rejectOversizedBody();
            return;
        }
        subscription.request(1);
    }

    @Override
    public void onNext(List<ByteBuffer> buffers) {
        if (result.isDone()) return;
        for (ByteBuffer buffer : buffers) {
            int chunkBytes = buffer.remaining();
            if (chunkBytes > maxResponseBytes - receivedBytes) {
                rejectOversizedBody();
                return;
            }
            receivedBytes += chunkBytes;
            while (buffer.hasRemaining()) {
                int copyBytes = Math.min(buffer.remaining(), copyBuffer.length);
                buffer.get(copyBuffer, 0, copyBytes);
                responseBody.write(copyBuffer, 0, copyBytes);
            }
        }
        subscription.request(1);
    }

    @Override
    public void onError(Throwable failure) {
        result.completeExceptionally(failure);
    }

    @Override
    public void onComplete() {
        if (!result.isDone()) result.complete(responseBody.toString(StandardCharsets.UTF_8));
    }

    private void rejectOversizedBody() {
        subscription.cancel();
        result.completeExceptionally(new ProviderRequestRejectedException("PROVIDER_RESPONSE_TOO_LARGE"));
    }
}
