package com.yeh35.practice.reactivestreams;


import io.reactivex.*;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableMaybeObserver;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

public class Program {

    public static void main(String[] args) throws Exception {
//        연산자();
//        flowable사용해보기();
//        observable사용해보기();
//        testCompositeDisposable();
        testMaybe();
    }

    public static void 연산자() {
        Flowable<Integer> flowable = Flowable.just(1, 2, 3, 4, 5) //인자를 순서대로 통지하는 생성자
                .filter(data -> data % 2 == 0) // 필터기능으로 true인 경우에만 통지된다.
                .map(data -> data * 100); //통지되는 데이터를 한건씩 받아 변환한다.

        flowable.subscribe(data -> System.out.println("data: " + data));
    }

    public static void flowable사용해보기() throws InterruptedException {

        Flowable<String> flowable =
                Flowable.create(new FlowableOnSubscribe<String>() {
                    @Override
                    public void subscribe(@NonNull FlowableEmitter<String> emitter) throws Exception {
                        String[] datas = {"Hello", "World!"};

                        for (String data : datas) {
                            if (emitter.isCancelled()) { // 구독이 해지되었는지 확인한다.
                                return;
                            }

                            System.out.println(emitter.hashCode() + ".onNext(" + data + ")");
                            emitter.onNext(data); //데이터 통지
                        }

                        System.out.println(emitter.hashCode() + ".onComplete()");
                        emitter.onComplete(); // 완료 통지
                    }
                }, BackpressureStrategy.BUFFER); //초과한 데이터는 버퍼링

        flowable.observeOn(Schedulers.computation()) //소비자가 실행될 스레드 설정(여기선 개별 스레드)
                .subscribe(new Subscriber<String>() {
                    private Subscription subscription; //데이터 개수 요청과, 구독 해지를 하는 개체

                    @Override
                    public void onSubscribe(Subscription s) {
                        this.subscription = s;
                        this.subscription.request(1L); // 받을 데이터 개수 요청
                    }

                    @Override
                    public void onNext(String data) {
                        String threadName = Thread.currentThread().getName();
                        System.out.println(threadName + " : " + data);
                        this.subscription.request(1L);
                    }

                    @Override
                    public void onError(Throwable t) {
                        t.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        String threadName = Thread.currentThread().getName();
                        System.out.println(threadName + " : 완료");
                    }
                });

        Thread.sleep(500L);
    }

    public static void observable사용해보기() throws InterruptedException {
        Observable<String> observable =
                Observable.create(new ObservableOnSubscribe<String>() {
                    @Override
                    public void subscribe(@NonNull ObservableEmitter<String> emitter) throws Exception {
                        String[] datas = {"Hello", "World!"};

                        for (String data : datas) {
                            if (emitter.isDisposed()) {
                                // 구독이 해지되면 처리를 중단
                                return;
                            }
                            emitter.onNext(data); //데이터 통지
                        }

                        emitter.onComplete(); // 완료 통지
                    }
                });

        observable.observeOn(Schedulers.computation()) // 소비자 개별 스레드 실행
                .subscribe(new Observer<String>() {
                    private Disposable disposable;

                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        disposable = d; // 구독 해지가 필요할 경우를 대배
                    }

                    @Override
                    public void onNext(@NonNull String data) {
                        String threadName = Thread.currentThread().getName();
                        System.out.println(threadName + " : " + data);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        String threadName = Thread.currentThread().getName();
                        System.out.println(threadName + " : 완료");
                    }
                });

        Thread.sleep(500L);
    }

    public static void testCompositeDisposable() throws Exception {
        // Disposable을 합친다.
        CompositeDisposable compositeDisposable = new CompositeDisposable();

        compositeDisposable.add(Flowable.range(1, 3) // 1부터 3까지 통지하는 생성자.
                .doOnCancel(() -> System.out.println("No.1 canceld")) //구독이 취소되는 경우
                .observeOn(Schedulers.computation())
                .subscribe(data -> {
                    Thread.sleep(100L);
                    System.out.println("No.1(" + Thread.currentThread().getName() + "):" + data);
                }));

        compositeDisposable.add(Flowable.range(1, 3)  // 1부터 3까지 통지하는 생성자.
                .doOnCancel(() -> System.out.println("No.2 canceld")) //구독이 취소되는 경우
                .observeOn(Schedulers.computation()) // 다른 쓰레드에서 실행
                .subscribe(data -> {
                    Thread.sleep(100L);
                    System.out.println("No.2(" + Thread.currentThread().getName() + "):" + data);
                }));

        Thread.sleep(250L);
        compositeDisposable.dispose();
    }

    public static void testSingle() {
        Single<DayOfWeek> single = Single.create(emitter -> {
            emitter.onSuccess(LocalDate.now().getDayOfWeek());
        });

        single.subscribe(new SingleObserver<DayOfWeek>() {

            @Override
            public void onSubscribe(@NonNull Disposable d) {
                // 아무것도 안함
            }

            @Override
            public void onSuccess(@NonNull DayOfWeek dayOfWeek) {
                System.out.println(dayOfWeek);
            }

            @Override
            public void onError(@NonNull Throwable e) {
                e.printStackTrace();
            }
        });
    }

    public static void testMaybe() {
        Maybe<DayOfWeek> maybe = Maybe.create(emitter -> {
            emitter.onSuccess(LocalDate.now().getDayOfWeek());
        });

        maybe.subscribe(new MaybeObserver<DayOfWeek>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {

            }

            @Override
            public void onSuccess(@NonNull DayOfWeek dayOfWeek) {
                System.out.println(dayOfWeek);
            }

            @Override
            public void onError(@NonNull Throwable e) {
                e.printStackTrace();
            }

            @Override
            public void onComplete() {
                System.out.println("완료");
            }
        });
    }


    public static void testCompletable() {
        Completable completable = Completable.create(emitter -> {
            // 어떤 작업~~
            emitter.onComplete();
        });

        completable
                .subscribeOn(Schedulers.computation())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onComplete() {
                        System.out.println("완료");
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        e.printStackTrace();
                    }
                });
    }

}
